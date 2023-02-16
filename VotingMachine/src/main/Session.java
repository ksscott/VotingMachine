package main;

import algorithm.Evaluator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import elections.games.Game;
import model.*;
import model.vote.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        SimpleRankingVote vote = new SimpleRankingVote(voterName);
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
        requireElection();

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

    private static final String DIR_PATH = "./data/";
    private static final String FILE_NAME = "votes.txt";

    public void saveDefaultVote(String voterName) throws IOException {
        requireElection();

        Vote vote = election.getVotes(race)
                .stream()
                .filter(v -> v.voterName.equals(voterName))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No vote found for voter name"));

        Path path = Paths.get(DIR_PATH + FILE_NAME);

        List<Vote> recordedVotes = new ArrayList<>();
        if (Files.exists(path)) {
            recordedVotes = Files.readAllLines(path)
                    .stream()
                    .map(this::deserializeVote)
                    .filter(v -> !vote.equals(v))
                    .collect(Collectors.toList());
        }
        recordedVotes.add(vote);

        Files.createDirectories(Path.of(DIR_PATH));
        Files.deleteIfExists(path);
        Files.write(path, serializeVotes(recordedVotes).getBytes());
    }

    public void loadDefaultVote(String voterName) throws IOException {
        requireElection();

        Path path = Paths.get(DIR_PATH + FILE_NAME);

        Vote vote = Files.readAllLines(path)
                .stream()
                .map(this::deserializeVote)
                .filter(Objects::nonNull)
                .filter(v -> v.voterName.equals(voterName))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Vote not found for " + voterName));

        addVote((RankedVote) vote); // FIXME

    }

    private void requireElection() {
        if (election == null) { throw new IllegalStateException("Start an election first"); }
    }

    private void updateRatings(String voterName) {
        requireElection();
        Map<Option, Integer> ratings = voterRatings.computeIfAbsent(voterName, k -> new HashMap<>());

        WeightedVote vote = new WeightedVote(voterName);
        for (Option option : ratings.keySet()) {
            vote.rate(option, ratings.get(option).doubleValue());
        }
        addVote(vote);
    }
    private static final List<Class> VOTE_TYPES = Arrays.asList(WeightedVote.class, SimpleRankingVote.class, SingleVote.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private Vote deserializeVote(String input) throws RuntimeException {
        try {
            for (Class type : VOTE_TYPES) {
                Vote vote = (Vote) mapper.readValue(input, type);
                if (vote == null) { continue; }
                return vote;
            }
        } catch (JsonProcessingException dbe) {
            throw new RuntimeException(dbe);
        }

        return null;
    }

    private String serializeVotes(List<Vote> votes) {
        try {
            StringJoiner joiner = new StringJoiner("\n");
            for (Vote vote : votes) {
                String s = mapper.writeValueAsString(vote);
                joiner.add(s);
            }
            return joiner.toString();
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException(jpe);
        }
    }
}
