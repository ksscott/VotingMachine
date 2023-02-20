package algorithm;

import model.Option;
import model.Race;
import model.vote.RankedVote;
import model.vote.Vote;
import model.vote.WeightedVote;

import java.util.*;
import java.util.stream.Collectors;

public class WeightedRunoff extends EvalAlgorithm<Vote> {

    private Map<Option, Double> standings;
    private Set<Vote> voters;
    private boolean multiRound = true; // True for "instant runoff" style; false for a single-round count

    public WeightedRunoff(Race race) {
        super(race);
    }

    @Override
    public Set<Option> evaluate(Set<Vote> votes) {
        initializeStandings(race.options());

        Set<Option> winners = null;
        this.voters = votes;

        voters.stream()
                .map(Vote::getVetoes)
                .flatMap(Set::stream)
                .forEach(standings::remove);

        int round = 1;
        if (multiRound) {
            while (winners == null) {
                System.out.println();
                System.out.println("EVALUATING ROUND: " + round++);
                winners = evaluateRound();
            }
        } else {
            winners = determineWinners();
        }

        return winners;
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
        // assign all voters
        caucus();

        // if strict majority, return winner
        Option strictWinner = strictWinner();
        if (strictWinner != null) {
            Set<Option> winningSet = new HashSet<>();
            winningSet.add(strictWinner);
            return winningSet;
        }

        // find candidate(s) with lowest score
        Set<Option> losingCandidates = losers();

        // if no losers / all losers, return all winners
        if (losingCandidates == null || losingCandidates.size() == standings.keySet().size()) {
            return standings.keySet();
        }

        // WARNING: if there's a tie for loser, this removes ALL losers
        for (Option loser : losingCandidates) {
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
            weightedVote.normalizeAcross(standings.keySet());
            for (Option option : standings.keySet()) {
                Double rating = weightedVote.getNormalizedRating(option);
                double unboxed = rating == null ? 0.0 : rating;
                standings.put(option, standings.get(option) + unboxed);
            }
        } else if (vote instanceof RankedVote rv) {
            for (Option option : rv.getRankings()) {
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

    /**
     * @return The {@link Option} that has a strict majority of votes; or else <code>null</code>
     */
    private Option strictWinner() {
        double scoreToWin = voters.size() / 2.0;

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
}
