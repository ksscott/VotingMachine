package algorithm;

import model.Option;
import model.Race;
import model.vote.RankedVote;
import model.vote.WeightedVote;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DescendingPoints extends EvalAlgorithm<RankedVote> {

    private Map<Option,Double> scores;
    private Set<WeightedVote> votes;

    public DescendingPoints(Race race) {
        super(race);
        this.votes = new HashSet<>();
    }

    /**
     * Accepts a WeightedVote or else converts using {@link model.vote.WeightedVote#rateDescending(RankedVote)}}
     */
    @Override
    public Set<Option> evaluate(Set<RankedVote> rankedVotes) {
        this.votes = new HashSet<>();
        for (RankedVote vote : rankedVotes) {
            WeightedVote toAdd = (vote instanceof WeightedVote) ?
                    ((WeightedVote) vote) : WeightedVote.rateDescending(vote);
            votes.add(toAdd);
        }

        initializeStandings();

        caucus();

        return determineWinners();
    }

    private void initializeStandings() {
        scores = new HashMap<>();
        race.options().forEach(opt -> scores.put(opt, 0.0));
    }

    private void caucus() {
        for (WeightedVote vote : votes) {
            for (Option option : vote.getRankings()) {
                scores.put(option, scores.get(option) + vote.getNormalizedRating(option));
            }
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
}
