package algorithm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;

import org.javatuples.*;

import model.*;
import model.vote.*;

public class Evaluator {
	
	/**
	 * @return a RankedChoiceVote, using multiple choices to indicate tying winners of a race
	 */
	public static Map<Race,List<Triplet<EvaluationResult,Double,Set<Option>>>> evaluateSingle(Election<Vote> election) {
		return evaluateElection(election, SingleChoice::new);
	}

	public static Map<Race,List<Triplet<EvaluationResult,Double,Set<Option>>>> evaluateRankedChoice(Election<RankedVote> election) {
		return evaluateElection(election, WeightedRunoff::new);
	}

	public static <V extends Vote> Map<Race,List<Triplet<EvaluationResult,Double,Set<Option>>>> evaluateElection(Election<V> election, Function<Race,EvalAlgorithm<V>> algorithm) {
		Ballot ballot = election.ballot;
		Map<Race, List<Triplet<EvaluationResult,Double,Set<Option>>>> result = new HashMap<>();

		for (Race race : ballot.races()) {
			Set<V> votes = election.getVotes(race);
			List<Triplet<EvaluationResult,Double,Set<Option>>> roundresults = algorithm.apply(race).evaluate(votes);
			result.put(race, roundresults);
		}
		
		return result;
	}
}
