package main;

import algorithm.Evaluator;
import elections.games.Game;
import model.*;
import model.vote.RankedVote;
import model.vote.SimpleRankingVote;
import model.vote.WeightedVote;

import java.util.*;
import java.util.stream.Collectors;

public class Session { // TODO threading issues?
    private Election<RankedVote> election;
    private Race race; // FIXME ?
    private Map<String,Map<Option,Integer>> voterRatings;

    public void startElection() {
        Set<Option> options = Game.shortList()
                .stream()
                .map(Game::getTitle)
                .map(Option::new)
                .collect(Collectors.toSet());
        race = new Race("Game", options);
        Ballot ballot = new Ballot("Game to Play", race);
        election = new Election<>(ballot);
        voterRatings = new HashMap<>();
    }

    public void addVote(RankedVote vote) {
        requireElection();

        election.addVote(race, vote);
    }

    public void addVote(String voterName, List<Game> games) {
        requireElection();

        List<Option> orderedChoices = games.stream()
                .map(Game::getTitle)
                .map(Option::new)
                .collect(Collectors.toList());

        SimpleRankingVote vote = new SimpleRankingVote(race, voterName);
        vote.select(orderedChoices);
//        addVote(vote); // FIXME
        addVote(WeightedVote.fromVote(vote)); // Force a weighting
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

    public void rate(String voterName, Game game, int rating) {
        if (game == null) { throw new IllegalArgumentException("Game not recognized"); }

        Map<Option, Integer> ratings = this.voterRatings.get(voterName);
        if (ratings == null) {
            ratings = new HashMap<>();
            voterRatings.put(voterName, ratings);
        }
        ratings.put(new Option(game.getTitle()), rating);
        updateRatings(voterName);
    }

    public Set<Option> pickWinner() {
        requireElection();

        Map<Race,Set<Option>> result = Evaluator.evaluateRankedChoice(election);
        return result.get(race);
    }

    private void requireElection() {
        if (election == null) { throw new IllegalStateException("Start an election first"); }
    }

    private void updateRatings(String voterName) {
        requireElection();
        Map<Option, Integer> ratings = voterRatings.get(voterName);

        if (ratings == null) {
            ratings = new HashMap<>();
            voterRatings.put(voterName, ratings);
        }

        WeightedVote vote = new WeightedVote(race, voterName);
        for (Option option : ratings.keySet()) {
            vote.rate(option, ratings.get(option).doubleValue());
        }
        addVote(vote);
    }
}
