package main;

import java.util.*;
import java.util.stream.Collectors;

import elections.games.Game;
import model.Option;
import model.EvaluationResult;

import org.javatuples.*;

public class REPL {

	public static final List<String> STOP_COMMANDS =
			Arrays.asList(/*"stop",*/ "end", "quit", "kill", "exit", "make it stop", "please god", "why");

	public static void main(String[] args) {
		selectFriendsGame();
	}

	/**
	 * Select a game to play with friends
	 */
	private static void selectFriendsGame() {
		System.out.println("Choosing a game to play with friends");

		Scanner scanner = new Scanner(System.in);
		List<Game> gameList = Game.shortList();

		Session session = new Session();
		session.startElection();

		System.out.println();
		System.out.println("The options are:");
		int i = 1;
		for (Game game : gameList) {
			System.out.println(i++ + ") " + game.toString());
		}

		// cast votes
		System.out.println("Begin voting");
		int voteNum = 1;
		while (true) {
			// cast a vote
			System.out.println();
			System.out.println("Casting Vote #" + voteNum);

			List<Game> orderedChoices = new ArrayList<>();
			int selectionRank = 1;
			while(true) {
				System.out.println("Enter your selection for Rank " + selectionRank + ", or 'done' to finish");

				String input = scanner.nextLine().trim();
				if (shouldQuit(input)) { return; }
				if ("done".equalsIgnoreCase(input)) {
					break;
				}

				Optional<Game> inferred = Game.interpret(input);
				if (inferred.isPresent()) {
					orderedChoices.add(inferred.get());
					selectionRank++;
				} else {
					System.out.println("Game title not recognized; please enter the name of a game");
				}
			}
			session.addVote("Voter #"+voteNum++, orderedChoices);

			System.out.println("Done casting votes? Type 'done' to finish");
			String input = scanner.nextLine().trim();
			if (shouldQuit(input)) { return; }
			if ("done".equalsIgnoreCase(input) || "yes".equalsIgnoreCase(input) || "y".equalsIgnoreCase(input)) {
				break;
			}
		}

		// determine winner(s)
		List<Triplet<EvaluationResult,Double,Set<Option>>> roundresults = session.pickWinner();
		StringBuffer winnersString = new StringBuffer();
		for (Triplet<EvaluationResult,Double,Set<Option>> result : roundresults) {
			if (result.getValue0() == EvaluationResult.WINNERS) {
				winnersString.append("WINNERS(score>"+result.getValue1().toString()+"): " + result.getValue2().stream().map(Option::name).sorted().collect(Collectors.joining(", and "))+"\n");
			} else {
				winnersString.append("ELIMINATED(score<"+result.getValue1().toString()+"): " + result.getValue2().stream().map(Option::name).sorted().collect(Collectors.joining(", and "))+"\n");
			}
		}
		// List<String> winningNames = session.pickWinner()
		// 		.stream()
		// 		.map(Option::name)
		// 		.sorted()
		// 		.collect(Collectors.toList());

		System.out.println();
		System.out.println("The winner is: " + String.join(", and ", winnersString));

		scanner.close();
	}

	private static boolean shouldQuit(String input) {
		return STOP_COMMANDS.contains(input.trim().toLowerCase());
	}
}
