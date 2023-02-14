package algorithm;

import model.Option;
import model.Race;
import model.vote.RankedVote;
import model.vote.WeightedVote;

import java.util.*;
import java.util.stream.Collectors;

public class WeightedRunoff extends EvalAlgorithm<RankedVote> {

    private Map<Option, Double> standings;
    private Set<RankedVote> voters;

    public WeightedRunoff(Race race) {
        super(race);
    }

    @Override
    public Set<Option> evaluate(Set<RankedVote> votes) {
        initializeStandings();
        System.out.println("Initializing standings...");

        this.voters = votes;

        Set<Option> winners = null;
        while (winners == null) {
            System.out.println("Evaluating round...");
            winners = evaluateRound();
        }

        return winners;
    }

    private void initializeStandings() {
        this.standings = new HashMap<>();
        race.options().forEach(option -> standings.put(option, 0.0));
    }

    /**
     * @return winners, if they have yet been found, null otherwise
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

        // if no losers, return all winners
        if (losingCandidates == null) {
            return standings.keySet();
        }

        // WARNING: if there's a tie for loser, this removes ALL losers
        for (Option loser : losingCandidates) {
//            System.out.println("Removing loser: " + loser.name);
            // drop the candidate
            standings.remove(loser);
        }

        return null; // loop again
    }

    // assign unassigned voters
    private void caucus() {
        // reset scores
        standings.keySet().forEach(option -> standings.put(option, 0.0));

        for (RankedVote vote : voters) {
            if (vote instanceof WeightedVote weightedVote) {
                weightedVote.normalizeAcross(standings.keySet());
                for (Option option : standings.keySet()) {
                    Double rating = weightedVote.getNormalizedRating(option);
                    double unboxed = rating == null ? 0.0 : rating;
                    standings.put(option, standings.get(option) + unboxed);
                }
            } else {
                for (Option option : vote.getRankings()) {
                    if (standings.containsKey(option)) {
                        standings.put(option, standings.get(option) + 1.0);
                        break;
                    }
                }
            }
        }
    }

    private Option strictWinner() {
        double scoreToWin = voters.size() / 2.0;

        return standings.keySet()
                .stream()
                .filter(candidate -> standings.get(candidate) > scoreToWin)
                .findAny()
                .orElse(null);
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
