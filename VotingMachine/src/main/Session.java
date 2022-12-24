package main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import algorithm.Evaluator;
import elections.games.Game;
import model.Ballot;
import model.Election;
import model.Option;
import model.Race;
import model.Vote.RankedChoiceVote;
import model.Vote.SingleVote;

public class Session {

	public static void main(String[] args) {
		// TODO
		
		////////////////////////////////////////
		// Select a game to play with friends //
		////////////////////////////////////////
		
		Set<Race> races = new HashSet<>();
		Set<Option> games = new HashSet<>();
		for (Game game : Game.values()) {
			games.add(new Option(game.name()));
		}
		Race race = new Race("Game", games);
		races.add(race);
		Ballot ballot = new Ballot("Game to Play", races);
		
		Election<SingleVote> election = new Election<>(ballot);
		
		// votes
		RankedChoiceVote result = Evaluator.evaluateSingle(election);
		List<Option> winningGames = result.getVote(race);
		
		System.out.println("The winner is: " + String.join(", and ", winningGames.stream().map(Option::toString).collect(Collectors.toList())));
	}

}
