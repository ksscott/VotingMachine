package algorithm;

import model.Option;
import model.Race;
import model.vote.SimpleRankingVote;

import java.util.*;
import java.util.stream.Collectors;

public class CopelandMethod extends EvalAlgorithm<SimpleRankingVote> {

    // WARNING: This method has a problem where voting [A,B,C] and [D]
    // gives fewer points to D than A because vote 1 puts B & C over D

    private Map<Option,Map<Option,ScorePair>> simulatedHeadToHeads;
    private Map<Option, Double> copelandScores;

    public CopelandMethod(Race race) {
        super(race);
    }

    @Override
    public Set<Option> evaluate(Set<SimpleRankingVote> votes) {
        initializeStandings();

        simulateMatchups(votes, new ArrayList<>(race.options()));

        calculateCopelandScores();

        return determineWinners();
    }

    protected void initializeStandings() {
        simulatedHeadToHeads = new HashMap<>();
        for (Option candidate : race.options()) {
            simulatedHeadToHeads.put(candidate, new HashMap<>());
            copelandScores = new HashMap<>();
        }
    }

    private void simulateMatchups(Set<SimpleRankingVote> votes, List<Option> candidates) {
        // iterate over candidate pairs
        for (int i = 0; i< candidates.size(); i++) {
            Option candidate = candidates.get(i);
            for (int j = i+1; j< candidates.size(); j++) {
                Option other = candidates.get(j);
                ScorePair score = new ScorePair();

                // iterate over votes
                for (SimpleRankingVote vote : votes) {
                    Option winner = score(candidate, other, vote);
                    if (candidate.equals(winner)) {
                        score.left = score.left + 1;
                    } else if (other.equals(winner)) {
                        score.right = score.right + 1;
                    }
                }

                simulatedHeadToHeads.get(candidate).put(other, score);
                simulatedHeadToHeads.get(other).put(candidate, score.invert());
            }
        }
    }

    private Option score(Option candidate, Option other, SimpleRankingVote vote) {
        List<Option> choices = vote.getRankings();

        int candidateRank = choices.indexOf(candidate);
        int otherRank = choices.indexOf(other);

        if (candidateRank >= 0) {
            if (otherRank >= 0) {
                return candidateRank < otherRank ? candidate : other;
            } else {
                return candidate;
            }
        } else if (otherRank >= 0) {
            return other;
        }

        return null;
    }

    private void calculateCopelandScores() {
        for (Option candidate : simulatedHeadToHeads.keySet()) {
            double score = 0.0;

            Map<Option,ScorePair> candidateMatchups = simulatedHeadToHeads.get(candidate);
            for (Option opponent : candidateMatchups.keySet()) {
                CopelandUnit unit = scorePairToCopelandScore(candidateMatchups.get(opponent));
                score += unit.points;
            }

            System.out.println("Score " + score + " for game " + candidate.name());
            copelandScores.put(candidate, score);
        }
    }

    private CopelandUnit scorePairToCopelandScore(ScorePair pair) {
        if (pair.left > pair.right) {
            return CopelandUnit.WIN;
        } else if (pair.right > pair.left) {
            return CopelandUnit.LOSS;
        } else {
            return CopelandUnit.DRAW;
        }
    }

    private Set<Option> determineWinners() {
        Double winningScore = copelandScores.values()
                .stream()
                .max(Double::compareTo)
                .orElse(-1.0); // no winners
        // Assume the winning score exists
        return copelandScores.keySet()
                .stream()
                .filter(option -> winningScore.equals(copelandScores.get(option)))
                .collect(Collectors.toSet());
    }

    private static class ScorePair {
        private int left, right;

        public ScorePair invert() {
            ScorePair inverted = new ScorePair();
            inverted.left = this.right;
            inverted.right = this.left;
            return inverted;
        }
    }

    enum CopelandUnit {
        WIN(1.0),
        DRAW(0.5),
        LOSS(0.0),
        ;

        public final double points;

        CopelandUnit(double points) { this.points = points; }
    }
}
