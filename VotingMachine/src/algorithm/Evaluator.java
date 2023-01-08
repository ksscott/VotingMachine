package algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import model.Ballot;
import model.Election;
import model.Option;
import model.Race;
import model.Vote.RankedChoiceVote;
import model.Vote.SingleVote;

public class Evaluator {
	
	/**
	 * @return a RankedChoiceVote, using multiple choices to indicate tying winners of a race
	 */
	public static RankedChoiceVote evaluateSingle(Election<SingleVote> election) {
		Ballot ballot = election.ballot;
		RankedChoiceVote result = new RankedChoiceVote(ballot, "Result");

		for (Race race : ballot.getRaces()) {
			Set<Option> winner = singleEvaluateRace(race, election.getVotes());
			result.select(race, new ArrayList<>(winner));
		}
		
		return result;
	}


	// return a set of tied winners
	private static Set<Option> singleEvaluateRace(Race race, Set<SingleVote> votes) {
		Map<Option, Integer> count = new HashMap<>();
		race.getOptions().forEach(option -> count.put(option, 0));

		for (SingleVote vote : votes) {
			Option choice = vote.getVote(race);
			if (choice != null) {
				count.put(choice, count.get(choice)+1);
			}
		}

		Set<Option> winners = new HashSet<>();
		int highest = 0;
		for (Option option : count.keySet()) {
			int number = count.get(option);
			if (number > highest) {
				winners = new HashSet<>();
				winners.add(option);
				highest = number;
			} else if (number == highest) {
				winners.add(option);
			}
		}

		return winners;
	}
	
	public static RankedChoiceVote evaluateRankedChoice(Election<RankedChoiceVote> election, EvalAlgorithm algorithm) {
		Ballot ballot = election.ballot;
		RankedChoiceVote result = new RankedChoiceVote(ballot, "Result");
		
		for (Race race : ballot.getRaces()) {
			Set<RankedChoiceVote> votes = election.getVotes();
			Set<Option> winner = algorithm.evaluate(votes);
			result.select(race, new ArrayList<>(winner));
		}
		
		return result;
	}
}
