package algorithm;

import model.Ballot;
import model.Election;
import model.Option;
import model.Race;
import model.vote.Vote;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class Evaluator {
	
	/**
	 * @return a RankedChoiceVote, using multiple choices to indicate tying winners of a race
	 */
	public static Map<Race,Set<Option>> evaluateSingle(Election<Vote> election) {
		return evaluateElection(election, SingleChoice::new);
	}

	public static Map<Race,Set<Option>> evaluateRankedChoice(Election<Vote> election) {
		return evaluateElection(election, WeightedRunoff::new);
	}

	public static <V extends Vote> Map<Race,Set<Option>> evaluateElection(Election<V> election, Function<Race,EvalAlgorithm<V>> algorithm) {
		Ballot ballot = election.ballot;
		Map<Race, Set<Option>> result = new HashMap<>();

		for (Race race : ballot.races()) {
			Set<V> votes = election.getVotes(race);
			Set<Option> winners = algorithm.apply(race).evaluate(votes);
			result.put(race, winners);
		}
		
		return result;
	}
}
