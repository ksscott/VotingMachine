package main;

import java.util.*;
import java.util.stream.Collectors;

import algorithm.CopelandMethod;
import algorithm.EvalAlgorithm;
import algorithm.Evaluator;
import elections.games.Game;
import model.Ballot;
import model.Election;
import model.Option;
import model.Race;
import model.Vote.RankedChoiceVote;
import model.Vote.SingleVote;

public class Session {

	public static final List<String> STOP_COMMANDS = Arrays.asList(new String[] {
			/*"stop",*/ "end", "quit", "kill", "exit", "make it stop", "please god", "why" });

	public static void main(String[] args) {
		// TODO
		
		////////////////////////////////////////
		// Select a game to play with friends //
		////////////////////////////////////////

		testFriendsGame();

	}

	private static void testFriendsGame() {
		System.out.println("Choosing a game to play with friends");

		Scanner scanner = new Scanner(System.in);

		Set<Option> games = new HashSet<>();
		Game[] gameList = Game.values();
		for (Game game : gameList) {
			games.add(new Option(game.name()));
		}
		Race race = new Race("Game", games);
		Ballot ballot = new Ballot("Game to Play", race);
		Election<RankedChoiceVote> election = new Election<>(ballot);

		System.out.println();
		System.out.println("The options are:");
		int i = 1;
		for (Game game : gameList) {
			System.out.println(i++ + ") " + game.toString());
		}

		// cast votes
		System.out.println("Begin voting");
		boolean allVotesCast = false;
		int voteNum = 0;
		while (!allVotesCast) {
			// cast a vote
			System.out.println();
			System.out.println("Casting Vote #" + ++voteNum);
			RankedChoiceVote vote = new RankedChoiceVote(ballot, "Voter #"+voteNum);
			List<Option> orderedChoices = new ArrayList<>();
			boolean thisVoteCast = false;
			int selectionRank = 0;
			while(!thisVoteCast) {
				System.out.println("Enter the index of your selection for Rank " + ++selectionRank + ", or 'done' to finish");

				String input = scanner.nextLine().trim();
				if (shouldQuit(input)) { return; }
				if ("done".equalsIgnoreCase(input)) {
					vote.select(race, orderedChoices);
					thisVoteCast = true;
					break;
				}

				int index = -1;
				try {
					index = Integer.parseInt(input);
				} catch (NumberFormatException e) {
					System.out.println("Unrecognized selection; please enter a number");
					continue;
				}
				if (index < 1 || index > gameList.length) {
					System.out.println("Index must be a number between 1 and " + gameList.length);
					continue;
				}
				orderedChoices.add(new Option(gameList[index-1].name()));
				System.out.println("Voting for: " + gameList[index-1].name());
			}
			election.addVote(vote);

			System.out.println("Done casting votes? Type 'done' to finish");
			String input = scanner.nextLine().trim();
			if (shouldQuit(input)) { return; }
			if ("done".equalsIgnoreCase(input) || "yes".equalsIgnoreCase(input)) {
				allVotesCast = true;
				break;
			}
		}

		// determine winner(s)
		RankedChoiceVote result = Evaluator.evaluateRankedChoice(election, CopelandMethod::new);
		List<Option> winningGames = result.getVote(race);

		System.out.println();
		System.out.println("The winner is: " + String.join(", and ", winningGames.stream().map(Option::toString).sorted().collect(Collectors.toList())));


		scanner.close();
	}

	private static boolean shouldQuit(String input) {
		return STOP_COMMANDS.contains(input.trim().toLowerCase());
	}
}
