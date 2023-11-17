package algorithm;

import model.Option;
import model.Race;
import model.Result;
import model.vote.RankedVote;
import model.vote.Vote;
import model.vote.WeightedVote;
import org.jetbrains.annotations.NotNull;
import org.jfree.data.flow.DefaultFlowDataset;

import java.util.*;
import java.util.stream.Collectors;

public class WeightedRunoff extends EvalAlgorithm<Vote> {

    private boolean multiRound = true; // True for "instant runoff" style; false for a single-round count
    private int round;
    private Map<Option, Double> standings;
    private Set<Vote> voters;
    Set<Option> latestLosers;
    Map<Option, Map<Option,Double>> latestFlows;
    DefaultFlowDataset<ScoredOption> resultsData;

    public WeightedRunoff(Race race) {
        super(race);
    }

    @Override
    public Result evaluate(Set<Vote> votes) {
        initializeStandings(race.options());
        this.latestLosers = new HashSet<>();
        this.latestFlows = new HashMap<>();
        this.resultsData = new DefaultFlowDataset<>();

        Set<Option> winners = null;
        this.voters = votes;

        voters.stream()
                .map(Vote::getVetoes)
                .flatMap(Set::stream)
                .forEach(standings::remove);

        this.round = 0;
        if (multiRound) {
            while (winners == null) {
                System.out.println();
                System.out.println("EVALUATING ROUND: " + ++round);
                winners = evaluateRound();
            }
        } else {
            winners = determineWinners();
        }

        return new Result(winners, resultsData);
    }

    /**
     * Change evaluation method between single-round and multi-round race.
     * In a multi-round race, votes for losing candidates are re-allocated each round.
     * WeightedVotes are re-normalized each round to preserve voting power.
     *
     * @param multiRound <code>true</code> for an Instant Runoff style race,
     *                   <code>false</code> to evaluate in a single round of vote counting
     */
    public void setInstantRunoff(boolean multiRound) {
        this.multiRound = multiRound;
    }

    private void initializeStandings(Collection<Option> remainingCandidates) {
        this.standings = new HashMap<>();
        remainingCandidates.forEach(option -> standings.put(option, 0.0));
    }

    /**
     * @return winners, if they have yet been found, or else <code>null</code>>
     */
    private Set<Option> evaluateRound() {
        // update flows from surviving candidates
        if (!latestLosers.isEmpty()) {
            latestFlows = new HashMap<>();
            for (Option candidate : standings.keySet()) {
                HashMap<Option, Double> map = new HashMap<>();
                map.put(candidate, standings.get(candidate));
                latestFlows.put(candidate, map);
            }
            latestLosers.forEach(loser -> latestFlows.put(loser, new HashMap<>()));
        }

        // assign all voters
        caucus();

        if (!latestLosers.isEmpty()) {
            recordFlows();
        }

        // if strict majority, return winner
        Option strictWinner = strictWinner();
        if (strictWinner != null) {
            Set<Option> winningSet = new HashSet<>();
            winningSet.add(strictWinner);
            return winningSet;
        }

        // find candidate(s) with lowest score
        latestLosers = losers();

        // if no losers / all losers, return all winners
        if (latestLosers == null || latestLosers.size() == standings.keySet().size()) {
            return standings.keySet();
        }

        // WARNING: if there's a tie for loser, this removes ALL losers
        for (Option loser : latestLosers) {
            // drop the candidate
            standings.remove(loser);
        }

        return null; // no winner yet; loop again
    }

    // assign unassigned voters
    private void caucus() {
        // reset scores
        initializeStandings(standings.keySet());

        voters.forEach(this::tallyVote);

        for (Option option : standings.keySet()) {
            System.out.println(option.name() + ": " + standings.get(option));
        }
    }

    /**
     * Distribute the voting power of the given vote to the remaining candidates.
     * Each vote is given a total weight of 1.0
     * @param vote The vote to be recorded
     */
    private void tallyVote(Vote vote) {
        if (vote instanceof WeightedVote weightedVote) {
            if (!weightedVote.isShadow()) {
                Map<Option,Double> loserRatings = new HashMap<>();
                for (Option loser : latestLosers) {
                    Double oldRating = weightedVote.getNormalizedRating(loser);
                    if (oldRating != null) {
                        loserRatings.put(loser, oldRating);
                    }
                }
                weightedVote.normalizeAcross(standings.keySet());

                // record flows from losers to survivors
                for (Option loser : loserRatings.keySet()) {
                    Map<Option, Double> map = latestFlows.get(loser);
                    if (map == null) { continue; }
                    for (Option survivor : standings.keySet()) {
                        Double newRating = weightedVote.getNormalizedRating((survivor));
                        if (newRating == null) { continue; }

                        Double flow = newRating * Math.abs(loserRatings.get(loser));

                        Double existingFlow = map.get(survivor);
                        existingFlow = existingFlow == null ? 0.0 : existingFlow;

                        map.put(survivor, flow + existingFlow);
                    }
                }
            }
            for (Option option : standings.keySet()) {
                Double rating = weightedVote.getNormalizedRating(option);
                if (weightedVote.isShadow()) {
                    rating = weightedVote.getRawRating(option); // use raw instead
                }
                double unboxed = rating == null ? 0.0 : rating;
                standings.put(option, standings.get(option) + unboxed);
            }
        } else if (vote instanceof RankedVote rv) {
            Option previousVote = null;
            for (Option option : rv.getRankings()) {
                if (latestLosers.contains(option) && previousVote == null) {
                    previousVote = option;
                }
                if (standings.containsKey(option)) {
                    standings.put(option, standings.get(option) + 1.0);

                    break;
                }
            }
        } else {
            Option selection = vote.toSingleVote().getVote();
            if (standings.containsKey(selection)) {
                standings.put(selection, standings.get(selection) + 1.0);
            }
        }
    }

    private void recordFlows() {
        List<ScoredOption> lastTos = resultsData.getDestinations(round - 2);
        for (Option from : latestFlows.keySet()) {
            Map<Option, Double> map = latestFlows.get(from);
            ScoredOption lastTo = lastTos.stream().filter(to -> from.name().equals(to.option.name())).findAny().orElse(null);
            for (Option to : map.keySet()) {
                resultsData.setFlow(round-1,
                        new ScoredOption(from, lastTo == null ? 0.0 : lastTo.score()),
                        new ScoredOption(to, standings.get(to)),
                        map.get(to));
            }
        }
    }

    /**
     * @return The {@link Option} that has a strict majority of votes; or else <code>null</code>
     */
    private Option strictWinner() {
        double scoreToWin = voters.stream().mapToDouble(vote -> {
            if (vote.isShadow()) {
                if (vote instanceof WeightedVote weightedVote) {
                    return standings.keySet()
                            .stream()
                            .mapToDouble(o -> {
                                Double d = weightedVote.getRawRating(o);
                                return d == null ? 0.0 : d;
                            })
                            .sum();
                }
            } else { // not shadow
                return 1.0; // assumption: non-shadow votes have a weight of 1.0
            }
            return 0.0;
        }).sum();

        return standings.keySet()
                .stream()
                .filter(candidate -> standings.get(candidate) > scoreToWin)
                .findAny()
                .orElse(null);
    }

    private Set<Option> determineWinners() {
        Double winningScore = standings.values()
                .stream()
                .max(Double::compareTo)
                .orElse(-1.0); // no winners
        // Assume the winning score exists
        return  standings.keySet()
                .stream()
                .filter(option -> winningScore.equals(standings.get(option)))
                .collect(Collectors.toSet());
    }

    private Set<Option> losers() {
        double lowestScore = standings.values()
                .stream()
                .min(Double::compareTo)
                .orElse(Double.MAX_VALUE);
        Set<Option> losers = standings.keySet()
                .stream()
                .filter(candidate -> standings.get(candidate) <= lowestScore)
                .collect(Collectors.toSet());
        // check for: all winners, no losers
        return losers.size() == standings.size() ? null : losers;
    }

    static record ScoredOption(@NotNull Option option, @NotNull Double score) implements Comparable<ScoredOption> {
        @Override
        public int compareTo(@NotNull ScoredOption o) {
            return this.score.compareTo(o.score);
        }

        @Override
        public String toString() {
            return option().name();
        }
    }
}
