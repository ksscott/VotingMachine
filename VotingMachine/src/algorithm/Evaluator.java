package algorithm;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Function;

import model.*;
import model.RankedChoiceVote;
import model.SingleVote;

public class Evaluator {
	
	/**
	 * @return a RankedChoiceVote, using multiple choices to indicate tying winners of a race
	 */
	public static RankedChoiceVote evaluateSingle(Election<SingleVote> election) {
		return evaluateElection(election, SingleChoice::new);
	}

	public static RankedChoiceVote evaluateRankedChoice(Election<RankedChoiceVote> election) {
		return evaluateElection(election, CopelandMethod::new);
	}

	public static <V extends Vote> RankedChoiceVote evaluateElection(Election<V> election, Function<Race,EvalAlgorithm<V>> algorithm) {
		Ballot ballot = election.ballot;
		RankedChoiceVote result = new RankedChoiceVote(ballot, "Result");
		
		for (Race race : ballot.races()) {
			Set<V> votes = election.getVotes();
			Set<Option> winner = algorithm.apply(race).evaluate(votes);
			result.select(race, new ArrayList<>(winner));
		}
		
		return result;
	}
}
