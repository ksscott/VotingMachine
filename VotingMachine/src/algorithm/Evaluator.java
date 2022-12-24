package algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
	
	public static RankedChoiceVote evaluateRankedChoice(Election<RankedChoiceVote> election) {
		Ballot ballot = election.ballot;
		RankedChoiceVote result = new RankedChoiceVote(ballot, "Result");
		
		for (Race race : ballot.getRaces()) {
			Set<Option> winner = rankedEvaluateRace(race, election.getVotes());
			result.select(race, new ArrayList<>(winner));
		}
		
		return result;
	}
	
	// return a set of tied winners
	public static Set<Option> singleEvaluateRace(Race race, Set<SingleVote> votes) {
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
	
	// return a set of tied winners
	public static Set<Option> rankedEvaluateRace(Race race, Set<RankedChoiceVote> votes) {
		return new RankedChoiceRace(race).evaluate(votes);
	}
	
	public static class RankedChoiceRace {
		
		private final Race race;
		private Map<Option,Set<RankedChoiceVote>> standings;
		private Set<RankedChoiceVote> unassignedVoters;

		public RankedChoiceRace(Race race) {
			this.race = race;
		}
		
		// return a set of tied winners
		public Set<Option> evaluate(Set<RankedChoiceVote> votes) {
			initializeStandings();
			
			this.unassignedVoters = votes;
			
			Set<Option> winners = null;
			while (winners == null) {
				winners = evaluateRound();
			}
			
			return winners;
		}
		
		private void initializeStandings() {
			this.standings = new HashMap<>();
			race.getOptions().forEach(option -> standings.put(option, new HashSet<>()));
		}
		
		/**
		 * @return winners, if they have yet been found, null otherwise
		 */
		private Set<Option> evaluateRound() {
			// assign unassigned voters
			caucus();
			
			// if strict majority, return winner
			Option strictWinner = strictWinner();
			if (strictWinner != null) {
				Set<Option> winningSet = new HashSet<>();
				winningSet.add(strictWinner);
				return winningSet;
			}
			
			// find candidate(s) with least votes
			Set<Option> losingCandidates = losers();
			
			// if no losers, return all winners
			if (losingCandidates == null) {
				return standings.keySet();
			}
			
			// WARNING: if there's a tie for loser, this removes ALL losers
			for (Option loser : losingCandidates) {
				// unassign their voters
				unassignedVoters.addAll(standings.get(loser));
				// drop the candidate
				standings.remove(loser);
			}
			
			return null; // loop again
		}
		
		// assign unassigned voters
		private void caucus() {
			for (RankedChoiceVote vote : unassignedVoters) {
				// assign vote
				List<Option> choices = vote.getVote(race);
				for (Option option : choices) {
					if (standings.containsKey(option)) {
						standings.get(option).add(vote);
						break;
					}
				}
			}
			unassignedVoters = new HashSet<>();
		}
		
		private Option strictWinner() {
			int remainingVoterCount = (int) standings.values().stream().flatMap(set -> set.stream()).count();
			
			for (Option candidate : standings.keySet()) {
				if (standings.get(candidate).size() > remainingVoterCount/2) {
					return candidate;
				}
			}
			return null;
		}
		
		private Set<Option> losers() {
			Map<Option, Integer> count = new HashMap<>();
			race.getOptions().forEach(option -> count.put(option, 0));
			
			for (Option candidate : standings.keySet()) {
				count.put(candidate, standings.get(candidate).size());
			}
			
			Set<Option> losers = new HashSet<>();
			int lowest = Integer.MAX_VALUE;
			for (Option option : count.keySet()) {
				int number = count.get(option);
				if (number < lowest) {
					losers = new HashSet<>();
					losers.add(option);
					lowest = number;
				} else if (number == lowest) {
					losers.add(option);
				}
			}
			
			if (losers.size() == count.keySet().size()) {
				return null; // all winners, no losers
			}
			
			return losers;
		}
	}
}
