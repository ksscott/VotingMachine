package model.vote;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.Option;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class WeightedVote extends RankedVote {

    @JsonProperty
    private Map<Option,Double> ratings;
    private Map<Option,Double> normalizedRatings;
    private Set<Option> filter;
    private boolean normalizedUpdated;

    @JsonCreator
    public WeightedVote(@JsonProperty("voterName") String voterName) {
        super(voterName);
        this.ratings = new HashMap<>();
        this.normalizedRatings = new HashMap<>();
        filter = new HashSet<>();
        normalizedUpdated = false;
    }

    public void rate(Option option, Double rating) {
        ratings.put(option, rating);
        normalizedUpdated = false;
        filter = new HashSet<>();
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

    public void normalizeAcross(@NotNull Set<Option> options) {
        filter = options;
        normalizedUpdated = false;
    }

    private void normalize() {
        if (normalizedUpdated) { return; }

        Double sum = ratings.keySet()
                .stream()
                .filter(filter::contains)
                .map(ratings::get)
                .map(Math::abs)
                .reduce(0.0, Double::sum);

        ratings.keySet()
                .stream()
                .filter(filter::contains)
                .forEach(option -> {
                    Double normalized = ratings.get(option) / sum;
                    normalizedRatings.put(option, normalized);
                });

        normalizedUpdated = true;
    }

    public static WeightedVote rateDescending(RankedVote vote) {
        WeightedVote result = new WeightedVote(vote.voterName);
        int rank = 1;
        for (Option option : vote.getRankings()) {
            double points = pointsForRank(rank++);
            result.rate(option, points);
        }
        return result;
    }

    public static WeightedVote fromVote(Vote vote) {
        if (vote instanceof WeightedVote wv) {
            return wv;
        } else if (vote instanceof RankedVote rv) {
            return rateDescending(rv);
        } else {
            SimpleRankingVote srv = SimpleRankingVote.fromVote(vote.toSingleVote());
            return rateDescending(srv);
        }
    }

    @Override
    public String toString() {
        String vetoesString = vetoes
                .stream()
                .map(Option::name)
                .collect(Collectors.joining(","));
        String ratingsString = ratings.keySet()
                .stream()
                .map(opt -> "(" + opt.name() + "," + ratings.get(opt) + ")")
                .collect(Collectors.joining(","));
        return "WeightedVote{ vetoes: {" + vetoesString + "}, ratings: {" + ratingsString + "}}";
    }

    private static double pointsForRank(int rank) {
        return 2.0/((double) (rank+1)); // 1->1.0, 2->0.67, 3->0.5, 4->0.4, 5->0.33, ...
    }
}
