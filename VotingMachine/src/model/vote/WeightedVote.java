package model.vote;

import model.Option;
import model.Race;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeightedVote extends RankedVote {

    Map<Option,Double> ratings;
    Map<Option,Double> normalizedRatings;
    private boolean normalizedUpdated;

    public WeightedVote(Race race, String voterName) {
        super(race, voterName);
        this.ratings = new HashMap<>();
        this.normalizedRatings = new HashMap<>();
        normalizedUpdated = false;
    }

    public void rate(Option option, Double rating) {
        if (!race.options().contains(option)) {
            throw new IllegalArgumentException("Option does not appear in this race");
        }
        ratings.put(option, rating);
        normalizedUpdated = false;
    }

    @Override
    public List<Option> getRankings() {
        return ratings.keySet()
                .stream()
                .sorted((o1, o2) -> Double.compare(ratings.get(o1), ratings.get(o2)))
                .collect(Collectors.toList());
    }

    public Double getRawRating(Option option) {
        return ratings.get(option);
    }

    /* As a fraction of 1.0 */
    public Double getNormalizedRating(Option option) {
        normalize();
        return normalizedRatings.get(option);
    }

    private void normalize() {
        if (normalizedUpdated) { return; }
        Double sum = ratings.values().stream().map(Math::abs).reduce(0.0, Double::sum);
        for (Option option : ratings.keySet()) {
            Double normalized = ratings.get(option) / sum;
            normalizedRatings.put(option, normalized);
        }
        normalizedUpdated = true;
    }

    public SimpleRankingVote toSimpleRankingVote() {
        SimpleRankingVote vote = new SimpleRankingVote(this.race, this.voterName);
        vote.select(getRankings());
        return vote;
    }

    public static WeightedVote rateDescending(RankedVote vote) {
        WeightedVote result = new WeightedVote(vote.race, vote.voterName);
        int rank = 1;
        for (Option option : vote.getRankings()) {
            double points = pointsForRank(rank++);
            result.rate(option, points);
        }
        return result;
    }

    private static double pointsForRank(int rank) {
        return 2.0/((double) (rank+1)); // 1->1.0, 2->0.67, 3->0.5, 4->0.4, 5->0.33, ...
    }
}
