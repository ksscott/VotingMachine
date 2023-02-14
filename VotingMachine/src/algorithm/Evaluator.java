package algorithm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import model.*;
import model.vote.*;

public class Evaluator {
	
	/**
	 * @return a RankedChoiceVote, using multiple choices to indicate tying winners of a race
	 */
	public static Map<Race,Set<Option>> evaluateSingle(Election<SingleVote> election) {
		return evaluateElection(election, SingleChoice::new);
	}

	public static Map<Race,Set<Option>> evaluateRankedChoice(Election<RankedVote> election) {
		return evaluateElection(election, DescendingPoints::new);
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
