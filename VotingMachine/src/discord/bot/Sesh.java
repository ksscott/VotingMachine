package discord.bot;

import algorithm.CopelandMethod;
import algorithm.Evaluator;
import elections.games.Game;
import model.*;

import java.util.*;
import java.util.stream.Collectors;

public class Sesh { // TODO threading issues?
    private Election<Vote.RankedChoiceVote> election;
    private Race race;

    public void startElection() {
        Set<Option> games = new HashSet<>();
        for (Game game : Game.shortList()) {
            games.add(new Option(game.title));
        }
        race = new Race("Game", games);
        Ballot ballot = new Ballot("Game to Play", race);
        election = new Election<>(ballot);
    }

    public void addVote(Vote.RankedChoiceVote vote) {
        if (election == null) { throw new IllegalStateException("Start an election first"); }

        election.addVote(vote);
    }

    public void addVote(String voterName, List<Game> games) {
        if (election == null) { throw new IllegalStateException("Start an election first"); }

        List<Option> orderedChoices = new ArrayList<>();
        for (Game game : games) {
            orderedChoices.add(new Option(game.title));
        }
        Vote.RankedChoiceVote vote = new Vote.RankedChoiceVote(election.ballot, voterName);
        vote.select(race, orderedChoices);
        addVote(vote);
    }

    public void addVote(String voterName, String... gameStrings) {
        if (election == null) { throw new IllegalStateException("Start an election first"); }

        List<Game> games = Arrays.stream(gameStrings)
                .map(input -> Game.interpret(input))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        addVote(voterName, games);
    }

    public List<Option> pickWinner() {
        if (election == null) {
            throw new IllegalStateException("Start an election first");
        }

        Vote.RankedChoiceVote result = Evaluator.evaluateRankedChoice(election, CopelandMethod::new);
        return result.getVote(race);
    }
}
