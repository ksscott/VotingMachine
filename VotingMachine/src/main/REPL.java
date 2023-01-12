package main;

import java.util.*;
import java.util.stream.Collectors;

import elections.games.Game;
import model.Option;

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
		Game[] gameList = Game.values();

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
		List<Option> winningGames = session.pickWinner();
		List<String> winningNames = winningGames.stream().map(Option::toString).sorted().collect(Collectors.toList());

		System.out.println();
		System.out.println("The winner is: " + String.join(", and ", winningNames));

		scanner.close();
	}

	private static boolean shouldQuit(String input) {
		return STOP_COMMANDS.contains(input.trim().toLowerCase());
	}
}
