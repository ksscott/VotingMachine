package algorithm;

import model.Ballot;
import model.Election;
import model.Race;
import model.Result;
import model.vote.Vote;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class Evaluator {
	
	/**
	 * @return a RankedChoiceVote, using multiple choices to indicate tying winners of a race
	 */
	public static Map<Race, Result> evaluateSingle(Election<Vote> election) {
		return evaluateElection(election, SingleChoice::new);
	}

	public static Map<Race,Result> evaluateRankedChoice(Election<Vote> election) {
		return evaluateElection(election, WeightedRunoff::new);
	}

	public static <V extends Vote> Map<Race,Result> evaluateElection(Election<V> election, Function<Race,EvalAlgorithm<V>> algorithm) {
		Ballot ballot = election.getBallot();
		Map<Race, Result> result = new HashMap<>();

		for (Race race : ballot.races()) {
			Set<V> votes = election.getVotes(race);
			Result raceResult = algorithm.apply(race).evaluate(votes);
			result.put(race, raceResult);
		}
		
		return result;
	}
}
