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
import org.jetbrains.annotations.NotNull;

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

    // Return true iff the voter is now currently vetoing the given option after this returns
    public boolean veto(String voterName, Option option) {
        requireElection();

        Vote vote = getVote(voterName);
        if (vote == null) {
            vote = new WeightedVote(voterName);
        }
        addVote((RankedVote) vote);

        return vote.vetoToggle(option);
    }

    public Set<Option> pickWinner() throws IOException {
        requireElection();

        Set<String> voters = election.getVotes(race, false)
                .stream()
                .map(v -> v.voterName)
                .collect(Collectors.toSet());
        loadUnspentVotes()
                .stream()
                .filter(v -> voters.contains(v.voterName))
                .forEach(v -> election.addVote(race, v));

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

        election.getVotes(race)
                .stream()
                .filter(v -> v.voterName.equals(voterName)) // vote and shadow vote
                .forEach(v -> election.removeVote(race, v));
    }

    /**
     * Clear the voter's existing {@link Session#saveDefaultVote(String) default vote}.
     * This is irreversible.
     * @param voterName
     */
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
        return getOptions()
                .stream()
                .sorted((o1, o2) -> {
                    String one = o1.name().toLowerCase();
                    String two = o2.name().toLowerCase();
                    if (one.startsWith(input.toLowerCase())) return -1;
                    if (two.startsWith(input.toLowerCase())) return +1;
                    if (one.contains(input)) return -1;
                    if (two.contains(input)) return +1;
                    return 0;
                })
                .findFirst();
//                .filter(option -> option.name().toLowerCase().contains(input.toLowerCase())).findAny();
    }

    public Vote getVote(String voterName) {
        requireElection();

        return election.getVotes(race, false)
                .stream()
                .filter(v -> v.voterName.equals(voterName))
                .findAny()
                .orElse(null);
    }

    /**
     * In consecutive elections, if some voter(s) always vote for unpopular options, their votes are "wasted".
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
        Set<WeightedVote> unspentVotes = election.getVotes(race, false)
                .stream()
                .map(v -> WeightedVote.unspentWeight(v, winner))
                .collect(Collectors.toSet());
        updateUnspentVotes(unspentVotes, winner);
    }

    public void setIncludeShadow(boolean includeShadow) { election.setIncludeShadow(includeShadow); }

    public int numVoters() { return election.getVotes(race, false).size(); }

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

    private Set<WeightedVote> loadUnspentVotes() throws IOException {
        Path path = Paths.get(DIR_PATH + UNSPENT_FILE_NAME);

        Set<WeightedVote> recordedVotes = new HashSet<>();
        if (Files.exists(path)) {
            recordedVotes = Files.readAllLines(path)
                    .stream()
                    .map(this::deserializeVote)
                    .filter(Objects::nonNull)
                    .map(WeightedVote::fromVote)
                    .collect(Collectors.toSet());
        }
        return recordedVotes;
    }

    private void updateUnspentVotes(@NotNull Set<WeightedVote> updates, @NotNull Option winner) throws IOException {
        // Get previously recorded votes
        Set<WeightedVote> recordedVotes = loadUnspentVotes();

        // Update recorded votes
        for (WeightedVote vote : recordedVotes) {
            WeightedVote update = updates
                    .stream()
                    .filter(vote::equals)
                    .findAny()
                    .orElse(null);
            // don't update an old vote if that person isn't here this time:
            if (update == null) { continue; }

            // all options rated by new and old votes
            Set<Option> superset = new HashSet<>(update.getRankings());
            superset.addAll(vote.getRankings());
            for (Option option : superset) {
                Double unspent = update.getRawRating(option); // RAW rating, not normalized rating
                if (unspent == null) { unspent = 0.0; }
                Double pastUnspent = vote.getRawRating(option); // RAW rating, not normalized rating
                if (pastUnspent == null) { pastUnspent = 0.0; }

                if (winner.equals(option)) {
                    if (pastUnspent > 0.0) { pastUnspent = 0.0; } // we got our wish
                } else {
                    if (pastUnspent < 0.0) { pastUnspent = 0.0; } // we got our wish
                }

                double newRating = unspent + pastUnspent;

                // reduce unspent weight unless voter reinforced it this election
                boolean votedAgainstPastVote = (pastUnspent > 0.0 && unspent <= 0.0) || (pastUnspent < 0.0 && unspent >= 0.0);
                if (votedAgainstPastVote) {
                    newRating *= 0.5;
                }

                // record or clear:
                if (newRating == 0.0) {
                    vote.clearRating(option);
                } else {
                    vote.rate(option, newRating);
                }
            }
        }
        // Add new, previously unrecorded votes:
        recordedVotes.addAll(updates);

        Path path = Paths.get(DIR_PATH + UNSPENT_FILE_NAME);
        Files.createDirectories(Path.of(DIR_PATH));
        Files.deleteIfExists(path);
        Files.write(path, serializeVotes(recordedVotes).getBytes());
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
