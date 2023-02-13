package main;

import algorithm.Evaluator;
import elections.games.Game;
import model.*;
import model.vote.RankedChoiceVote;

import java.util.*;
import java.util.stream.Collectors;

public class Session { // TODO threading issues?
    private Election<RankedChoiceVote> election;
    private Race race;

    public void startElection() {
        Set<Option> options = Game.shortList()
                .stream()
                .map(Game::getTitle)
                .map(Option::new)
                .collect(Collectors.toSet());
        race = new Race("Game", options);
        Ballot ballot = new Ballot("Game to Play", race);
        election = new Election<>(ballot);
    }

    public void addVote(RankedChoiceVote vote) {
        requireElection();

        election.addVote(race, vote);
    }

    public void addVote(String voterName, List<Game> games) {
        requireElection();

        List<Option> orderedChoices = games.stream()
                .map(Game::getTitle)
                .map(Option::new)
                .collect(Collectors.toList());

        RankedChoiceVote vote = new RankedChoiceVote(race, voterName);
        vote.select(orderedChoices);
        addVote(vote);
    }

    public void addVote(String voterName, String... gameStrings) {
        requireElection();

        List<Game> games = Arrays.stream(gameStrings)
                .map(Game::interpret)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        addVote(voterName, games);
    }

    public Set<Option> pickWinner() {
        requireElection();

        Map<Race,Set<Option>> result = Evaluator.evaluateRankedChoice(election);
        return result.get(race);
    }

    private void requireElection() {
        if (election == null) { throw new IllegalStateException("Start an election first"); }
    }
}
