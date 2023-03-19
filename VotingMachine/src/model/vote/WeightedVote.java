package model.vote;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.Option;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class WeightedVote extends RankedVote implements Cloneable {

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
        normalizedUpdated = false;
        filter = new HashSet<>();
    }

    @Override
    public WeightedVote clone() {
        WeightedVote clone = new WeightedVote(this.voterName);
        clone.ratings = new HashMap<>(this.ratings);
        clone.normalizeAcross(this.filter); // preserve current normalization state
        return clone;
    }

    public void rate(Option option, Double rating) {
        ratings.put(option, rating);
        normalizedUpdated = false;
        filter = new HashSet<>();
    }

    public void clearRating(Option option) {
        ratings.remove(option);
    }

    @Override
    public List<Option> getRankings() {
        return ratings.keySet()
                .stream()
                .sorted((o1, o2) -> Double.compare(ratings.get(o2), ratings.get(o1)))
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

    /**
     * In the event that the given vote "lost" a {@link model.Race race} to some degree,
     * this method measures the amount of the vote that was "wasted" or stifled.
     * <br>
     * Specifically, that is the normalized portion of this vote that was spent on
     * each option that the given vote preferred over the winner.
     * <br>
     * If the given vote voted against the winner (i.e. gave it a negative rating),
     * that rating is included in the vote's unspent weight.
     * A {@link SimpleRankingVote} gives all its weight to its highest-rated option.
     *
     * FIXME need to handle tied ratings, currently gives random behavior RE: whether ties are counted as unspent or not
     *
     * @param vote
     * @param winner The winning option in a recently completed {@link model.Race race}.
     * @return a mapping of unspent weightings for each preferred candidate.
     * These unspent weights are normalized, and should add to at most <code>1.0</code>.
     */
    public static WeightedVote unspentWeight(Vote vote, Option winner) {
        Map<Option,Double> unspent = new HashMap<>();
        WeightedVote retval;
        if (vote instanceof WeightedVote wv) {
            WeightedVote tempWeightedVote = wv.clone(); // Don't modify state of the original vote
            tempWeightedVote.normalizeAcross(tempWeightedVote.ratings.keySet()); // Remove normalization filter
            for (Option option : tempWeightedVote.getRankings()) { // in decreasing order of preference
                Double unspentPortion = tempWeightedVote.getNormalizedRating(option);
                if (winner.equals(option)) {
                    if (unspentPortion < 0.0) { // voted against option
                        // voting against winner is considered "unspent"
                        unspent.put(option, unspentPortion);
                    }
                    break; // finished; remaining options are considered "spent"
                } // else: this option lost
                if (unspentPortion > 0.0) {
                    // voted for this loser; record unspent weight
                    unspent.put(option, unspentPortion);
                } // else: voted against this loser; no unspent weight
            }
            tempWeightedVote.ratings = new HashMap<>();
            for (Option option : unspent.keySet()) {
                tempWeightedVote.rate(option, unspent.get(option));
            }
            retval = tempWeightedVote;
        } else {
            Option chosen = vote.toSingleVote().getVote();
            retval = new WeightedVote(vote.voterName);
            if (chosen != null && !chosen.equals(winner)) {
                retval.rate(chosen, 1.0);
                unspent.put(chosen, 1.0);
            }
        }

        retval.setShadow(true);
        retval.vetoes = new HashSet<>();
        return retval;
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
        String shadowString = this.shadow ? "(shadow)" : "";
        return "WeightedVote{ " + shadowString
                + " voter:" + voterName
                + " vetoes:{" + vetoesString
                + "}, ratings:{" + ratingsString + "}}";
    }

    private static double pointsForRank(int rank) {
        return 2.0/((double) (rank+1)); // 1->1.0, 2->0.67, 3->0.5, 4->0.4, 5->0.33, ...
    }
}
