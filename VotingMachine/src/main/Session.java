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
    private static final String VOTES_FILE_NAME = "votes.txt";
    private static final String UNSPENT_FILE_NAME = "unspent.txt";

    /**
     * Records the given voter's current vote in this election as their "default" vote.
     * A voter's default vote can be {@link Session#loadDefaultVote(String) loaded} for use
     * in future elections.
     * @param voterName
     * @throws IOException
     */
    public void saveDefaultVote(String voterName) throws IOException {
        requireElection();

        Vote vote = getVote(voterName);

        replaceDefaultVote(voterName, vote);
    }

    /**
     * Load the voter's existing {@link Session#saveDefaultVote(String) default vote}.
     * It replaces the voters current vote.
     * @param voterName
     * @throws IOException
     */
    public void loadDefaultVote(String voterName) throws IOException {
        requireElection();

        Path path = Paths.get(DIR_PATH + VOTES_FILE_NAME);

        Vote vote = Files.readAllLines(path)
                .stream()
                .map(this::deserializeVote)
                .filter(Objects::nonNull)
                .filter(v -> v.voterName.equals(voterName))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Vote not found for " + voterName));

        addVote((RankedVote) vote); // FIXME
    }

    /**
     * Clear the voter's current vote in this election.
     * This is irreversible.
     * @param voterName
     */
    public void clearCurrentVote(String voterName) {
        requireElection();

        Vote vote = election.getVotes(race)
                .stream()
                .filter(v -> v.voterName.equals(voterName))
                .findAny()
                .orElse(null);

        election.removeVote(race, vote);
    }

    /**
     * Clear the voter's existing {@link Session#saveDefaultVote(String) default vote}.
     * This is irreversible.
     * @param voterName
     */
    public void clearDefaultVote(String voterName) throws IOException {
        replaceDefaultVote(voterName, null);
    }

    /**
     * In consecutive elections, if some voter(s) always vote prefer unpopular options, their votes are "wasted".
     * In order to let them win occasionally, their preferences need to build in weight over time
     * according to how much of their vote they spent on eliminated options.
     * <br>
     * This method should be called <strong>only once</strong> after a winner is determined.
     * It records that "wasted" portion of votes so they can be heard in future elections. 
     * This portion is calculated according to {@link WeightedVote#unspentWeight(Vote, Option)}.
     * @param winner The option that won the most recent {@link Race race}
     * @throws IOException
     */
    public void recordUnspentVotes(Option winner) throws IOException {
        Set<Vote> votes = election.getVotes(race);
        Map<String,Map<Option,Double>> voterUnspentWeights = new HashMap<>();
        for (Vote vote : votes) {
            Map<Option, Double> unspentPortions = WeightedVote.unspentWeight(vote, winner);
            voterUnspentWeights.put(vote.voterName, unspentPortions);
        }
        updateUnspentVotes(voterUnspentWeights, winner);
    }

    private void replaceDefaultVote(String voterName, Vote vote) throws IOException {
        Path path = Paths.get(DIR_PATH + VOTES_FILE_NAME);

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

    private void updateUnspentVotes(Map<String,Map<Option,Double>> voterUnspentWeights, Option winner) throws IOException {
        Path path = Paths.get(DIR_PATH + UNSPENT_FILE_NAME);

        // Get previously recorded votes
        List<WeightedVote> recordedVotes = new ArrayList<>();
        if (Files.exists(path)) {
            recordedVotes = Files.readAllLines(path)
                    .stream()
                    .map(this::deserializeVote)
                    .filter(Objects::nonNull)
                    .map(WeightedVote::fromVote)
                    .collect(Collectors.toList());
        }
        // Add new, previously unrecorded votes:
        for (String name : voterUnspentWeights.keySet()) {
            if (name == null) { continue; }
            boolean recorded = recordedVotes
                    .stream()
                    .anyMatch(vote -> name.equals(vote.voterName));
            if (!recorded) {
                recordedVotes.add(new WeightedVote(name));
            }
        }
        // Update recorded votes
        for (WeightedVote vote : recordedVotes) {
            Map<Option, Double> unspentPortions = voterUnspentWeights.get(vote.voterName);
            if (unspentPortions == null) { continue; }
            Set<Option> superset = new HashSet<>(unspentPortions.keySet());
            superset.addAll(vote.getRankings());
            for (Option option : superset) {
                double newRating = 0.0;
                if (!winner.equals(option)) {
                    Double unspent = unspentPortions.get(option);
                    Double voteRating = vote.getRawRating(option); // RAW rating, not normalized rating
                    newRating = (unspent == null ? 0.0 : unspent) + (voteRating == null ? 0.0 : voteRating);
                }
                vote.rate(option, newRating);
            }
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
        } catch (JsonProcessingException jpe) {
            System.out.println("error deserializing vote: " + input);
            throw new RuntimeException("Error processing JSON", jpe);
        }

        return null;
    }

    private String serializeVotes(Collection<? extends Vote> votes) {
        try {
            StringJoiner joiner = new StringJoiner("\n");
            for (Vote vote : votes) {
                String s = mapper.writeValueAsString(vote);
                joiner.add(s);
            }
            return joiner.toString();
        } catch (JsonProcessingException jpe) {
            System.out.println("error serializing votes");
            throw new RuntimeException("Error processing JSON", jpe);
        }
    }
}
