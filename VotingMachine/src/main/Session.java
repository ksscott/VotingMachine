package main;

import algorithm.Evaluator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public void startElection(Set<Option> options) {
        race = new Race("Game", options);
        Ballot ballot = new Ballot("Game to Play", race);
        election = new Election<>(ballot);
    }

    public Set<Option> getOptions() { return race.options(); }

    public void addVote(RankedVote vote) {
        requireElection();

        election.addVote(race, vote);
    }

    public void addVote(String voterName, List<Option> orderedChoices) {
        requireElection();

        SimpleRankingVote vote = new SimpleRankingVote(voterName);
        vote.select(orderedChoices);
//        addVote(vote); // FIXME
        addVote(WeightedVote.fromVote(vote)); // Force a weighting
    }

    public void addVote(String voterName, String... gameStrings) {
        requireElection();

        List<Option> options = Arrays.stream(gameStrings)
                .map(this::interpret)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        addVote(voterName, options);
    }

    public void rate(String voterName, Option option, int rating) {
        requireElection();

        if (option == null || !race.options().contains(option)) {
            throw new IllegalArgumentException("Option not recognized");
        }

        Vote vote = getVote(voterName);
        if (vote == null || !(vote instanceof WeightedVote)) {
            vote = new WeightedVote(voterName);
        }
        ((WeightedVote) vote).rate(option, (double) rating);

        addVote((RankedVote) vote);
    }

    public void veto(String voterName, Option option) {
        requireElection();

        Vote vote = getVote(voterName);
        if (vote == null) {
            vote = new WeightedVote(voterName);
        }
        vote.veto(option);
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

    public void suggest(Option suggestion) {
         requireElection();

        Set<Option> options = new HashSet<>(race.options());
        options.add(suggestion);
        Race oldRace = race;
        race = new Race(race.name(), options);

        election.updateRace(oldRace, race);
    }

    public Optional<Option> interpret(String input) {
        return getOptions().stream().filter(option -> option.name().toLowerCase().contains(input.toLowerCase())).findAny();
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
