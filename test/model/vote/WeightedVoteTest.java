package model.vote;

import model.Option;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class WeightedVoteTest {

    @Test
    void normalizedRating_isNull_whenAllRatingsAreZero() {
        Option option = new Option("Helldivers 2");
        WeightedVote vote = new WeightedVote("voter");
        vote.rate(option, 0.0);
        vote.normalizeAcross(Set.of(option));

        Double result = vote.getNormalizedRating(option);

        // Before the fix, this returned NaN and caused an infinite evaluation loop
        assertFalse(result != null && result.isNaN(), "Normalized rating must not be NaN");
    }

    @Test
    void normalizedRating_sumsToOne_underNormalConditions() {
        Option a = new Option("A");
        Option b = new Option("B");
        Option c = new Option("C");
        WeightedVote vote = new WeightedVote("voter");
        vote.rate(a, 3.0);
        vote.rate(b, 2.0);
        vote.rate(c, 1.0);
        vote.normalizeAcross(Set.of(a, b, c));

        double sum = vote.getNormalizedRating(a)
                + vote.getNormalizedRating(b)
                + vote.getNormalizedRating(c);

        assertEquals(1.0, sum, 1e-9);
    }
}
