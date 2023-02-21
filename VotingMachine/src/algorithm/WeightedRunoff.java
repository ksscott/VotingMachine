package algorithm;

import model.Option;
import model.Race;
import model.vote.RankedVote;
import model.vote.WeightedVote;
import model.EvaluationResult;
import java.util.List;
import java.util.ArrayList;

import org.javatuples.*;

import java.util.*;
import java.util.stream.Collectors;

public class WeightedRunoff extends EvalAlgorithm<RankedVote> {

    private Map<Option, Double> standings;
    private Set<RankedVote> voters;
    private boolean multiRound = true; // True for "instant runoff" style; false for a single-round count

    public WeightedRunoff(Race race) {
        super(race);
    }

    @Override
    public List<Triplet<EvaluationResult,Double,Set<Option>>> evaluate(Set<RankedVote> votes) {
        initializeStandings();

        this.voters = votes;
        List<Triplet<EvaluationResult,Double,Set<Option>>> roundresults = new ArrayList<Triplet<EvaluationResult,Double,Set<Option>>>();
        Boolean winnerfound = false;

        if (multiRound) {
            while (!winnerfound) {
                Triplet<EvaluationResult,Double,Set<Option>> roundresult = evaluateRound();
                if (roundresult.getValue0() == EvaluationResult.WINNERS) {winnerfound = true;}
                roundresults.add(roundresult);
            }
        } else {
            Triplet<EvaluationResult,Double,Set<Option>> roundresult = determineWinners();
            roundresults.add(roundresult);
        }

        return roundresults;
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

    private void initializeStandings() {
        this.standings = new HashMap<>();
        race.options().forEach(option -> standings.put(option, 0.0));
    }

    /**
     * @return winners, if they have yet been found, null otherwise
     */
    private Triplet<EvaluationResult,Double,Set<Option>> evaluateRound() {
        // assign all voters
        caucus();

        // if strict majority, return winner
        Pair<Double,Option> strictWinner = strictWinner();
        if (strictWinner.getValue1() != null) {
            Set<Option> winningSet = new HashSet<>();
            winningSet.add(strictWinner.getValue1());
            return Triplet.with(EvaluationResult.WINNERS, strictWinner.getValue0(), winningSet);
        }

        // find candidate(s) with lowest score
        Pair<Double,Set<Option>> losingCandidates = losers();

        // if no losers / all losers, return all winners
        if (losingCandidates == null || losingCandidates.getValue1().size() == standings.keySet().size()) {
            return Triplet.with(EvaluationResult.WINNERS, losingCandidates.getValue0(), standings.keySet());
        }

        // WARNING: if there's a tie for loser, this removes ALL losers
        for (Option loser : losingCandidates.getValue1()) {
            // drop the candidate
            standings.remove(loser);
        }

        return Triplet.with(EvaluationResult.LOSERS, losingCandidates.getValue0(), losingCandidates.getValue1()); // loop again
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

    private Pair<Double, Option> strictWinner() {
        double scoreToWin = voters.size() / 2.0;

        return Pair.with(scoreToWin, standings.keySet()
                .stream()
                .filter(candidate -> standings.get(candidate) > scoreToWin)
                .findAny()
                .orElse(null));
    }

    private Triplet<EvaluationResult,Double,Set<Option>> determineWinners() {
        Double winningScore = standings.values()
                .stream()
                .max(Double::compareTo)
                .orElse(-1.0); // no winners
        // Assume the winning score exists
        return  Triplet.with(EvaluationResult.WINNERS, winningScore, standings.keySet()
                .stream()
                .filter(option -> winningScore.equals(standings.get(option)))
                .collect(Collectors.toSet()));
    }

    private Pair<Double, Set<Option>> losers() {
        double lowestScore = standings.values()
                .stream()
                .min(Double::compareTo)
                .orElse(Double.MAX_VALUE);
        Set<Option> losers = standings.keySet()
                .stream()
                .filter(candidate -> standings.get(candidate) <= lowestScore)
                .collect(Collectors.toSet());
        // check for: all winners, no losers
        return Pair.with(lowestScore, losers.size() == standings.size() ? null : losers);
    }
}
