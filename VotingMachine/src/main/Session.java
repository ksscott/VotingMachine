package main;

import algorithm.Evaluator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import elections.games.Game;
import model.Ballot;
import model.Election;
import model.Option;
import model.Race;
import model.vote.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Session { // TODO threading issues?
    private Election<Vote> election;
    private Race race; // FIXME ?

    public void startElection() {
        Set<Option> options = Game.shortList() // FIXME
                .stream()
                .map(Game::getTitle)
                .map(Option::new)
                .collect(Collectors.toSet());
        race = new Race("Game", options);
        Ballot ballot = new Ballot("Game to Play", race);
        election = new Election<>(ballot);
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

        Vote vote = getVote(voterName);
        if (vote == null || !(vote instanceof WeightedVote)) {
            vote = new WeightedVote(voterName);
        }
        ((WeightedVote) vote).rate(new Option(game.getTitle()), (double) rating);

        addVote((RankedVote) vote);
    }

    public void veto(String voterName, Game game) {
        requireElection();

        Vote vote = getVote(voterName);
        if (vote == null) {
            vote = new WeightedVote(voterName);
        }
        vote.veto(new Option(game.getTitle()));
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

        Vote vote = getVote(voterName);

        replaceDefaultVote(voterName, vote);
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

    public void clearCurrentVote(String voterName) {
        requireElection();

        Vote vote = election.getVotes(race)
                .stream()
                .filter(v -> v.voterName.equals(voterName))
                .findAny()
                .orElse(null);

        election.removeVote(race, vote);
    }

    public void clearDefaultVote(String voterName) throws IOException {
        replaceDefaultVote(voterName, null);
    }

    private void replaceDefaultVote(String voterName, Vote vote) throws IOException {
        Path path = Paths.get(DIR_PATH + FILE_NAME);

        List<Vote> recordedVotes = new ArrayList<>();
        if (Files.exists(path)) {
            recordedVotes = Files.readAllLines(path)
                    .stream()
                    .map(this::deserializeVote)
                    .filter(Objects::nonNull)
                    .filter(v -> !v.voterName.equals(voterName))
                    .collect(Collectors.toList());
        }
        if (vote != null) {
            recordedVotes.add(vote);
        }

        Files.createDirectories(Path.of(DIR_PATH));
        Files.deleteIfExists(path);
        Files.write(path, serializeVotes(recordedVotes).getBytes());
    }

    private Vote getVote(String voterName) {
        return election.getVotes(race)
                .stream()
                .filter(v -> v.voterName.equals(voterName))
                .findAny()
                .orElse(null);
    }

    private void requireElection() {
        if (election == null) { throw new IllegalStateException("Start an election first"); }
    }

    private static final List<Class> VOTE_TYPES = Arrays.asList(WeightedVote.class, SimpleRankingVote.class, SingleVote.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private Vote deserializeVote(String input) throws RuntimeException {
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
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
