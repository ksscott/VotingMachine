package algorithm;

import model.Option;
import model.Race;
import model.RankedChoiceVote;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DescendingPoints extends EvalAlgorithm<RankedChoiceVote> {

    private Map<Option,Double> scores;

    public DescendingPoints(Race race) {
        super(race);
    }

    @Override
    public Set<Option> evaluate(Set<RankedChoiceVote> votes) {
        System.out.println("Tallying votes:"); // votes are correct here
        votes.stream().map(v -> v.getVote(race)).forEach(System.out::println);
        initializeStandings();

        for (RankedChoiceVote vote : votes) {
            int rank = 1;
            for (Option option : vote.getVote(race)) {
                double points = pointsForRank(rank++);
                scores.put(option, scores.get(option) + points);
            }
        }

        return determineWinners();
    }

    private void initializeStandings() {
        scores = new HashMap<>();
        for (Option option : race.options()) {
            scores.put(option, 0.0);
        }
    }

    private Set<Option> determineWinners() {
        Double winningScore = scores.values()
                .stream()
                .max(Double::compareTo)
                .orElse(-1.0); // no winners
        // Assume the winning score exists
        return scores.keySet()
                .stream()
                .filter(option -> winningScore.equals(scores.get(option)))
                .collect(Collectors.toSet());
    }

    private double pointsForRank(int rank) {
        return 2.0/((double) (rank+1)); // 1->1.0, 2->0.67, 3->0.5, 4->0.4, 5->0.33, ...
    }
}
