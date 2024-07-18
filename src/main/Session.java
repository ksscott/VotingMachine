package main;

import algorithm.Evaluator;
import model.*;
import model.vote.SimpleRankingVote;
import model.vote.Vote;
import model.vote.WeightedVote;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.flow.FlowPlot;
import org.jfree.data.flow.DefaultFlowDataset;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Session { // TODO threading considerations
    private Election<Vote> election;
    private Race race; // FIXME

    private static final String DATA_DIR_PATH = "./data/";
    private static final Path VOTES_FILE_PATH = Path.of(DATA_DIR_PATH, "votes.txt");
    private static final Path UNSPENT_FILE_PATH = Path.of(DATA_DIR_PATH, "unspent.txt");
    private static final Path CHART_FILE_PATH = Path.of(DATA_DIR_PATH, "flowplot.png");
    private static final Path SAVED_OPTIONS_FILE_PATH = Path.of(DATA_DIR_PATH, "savedCandidates.txt");

    //region Election State

    /** @param options A set of candidates to vote for. If null, previously stored candidates will be loaded. */
    public void startElection(@Nullable String prompt, @Nullable Set<Option> options) {
        if (prompt == null || prompt.equals("")) prompt = "Election";
        if (options == null) {
            options = new HashSet<>();
            try {
                options = loadStoredCandidates();
            } catch (IOException e) {
                System.out.println("error loading saved options");
            }
        }

        race = new Race(prompt, options);
        Ballot ballot = new Ballot(prompt, race);
        election = new Election<>(ballot);
    }

    /** @return the set of candidates in this election */
    @NotNull
    public Set<Option> getOptions() {
        requireElection();
        return race.options();
    }

    @NotNull
    public Optional<Option> interpret(@Nullable String input) {
        if (input == null) return Optional.empty();
        return getOptions()
                .stream().min((o1, o2) -> {
                    String one = o1.name().toLowerCase();
                    String two = o2.name().toLowerCase();
                    if (one.startsWith(input.toLowerCase())) return -1;
                    if (two.startsWith(input.toLowerCase())) return +1;
                    if (one.contains(input)) return -1;
                    if (two.contains(input)) return +1;
                    return 0;
                });
    }

    /** Add a candidate to this election */
    public void suggest(@NotNull Option suggestion) {
        requireElection();

        Set<Option> options = new HashSet<>(race.options());
        options.add(suggestion);
        Race oldRace = race;
        race = new Race(race.name(), options);

        election.updateRace(oldRace, race);
    }

    public int numVoters() {
        requireElection();
        return election.getVotes(race, false).size();
    }

    @NotNull
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

        Map<Race, Result> results = Evaluator.evaluateRankedChoice(election);

        outputResultsChart(results.get(race).getData());

        return results.get(race).getWinners();
    }

    /**
     * @param data the chart data to draw
     * @param <T>
     * @throws IOException for errors during write of chart file
     */
    private <T extends Comparable<T>> void outputResultsChart(@NotNull DefaultFlowDataset<T> data) throws IOException {
        DataUtils.writeFile(CHART_FILE_PATH, "");

        FlowPlot plot = new FlowPlot(data);
        JFreeChart chart = new JFreeChart(plot);

        ChartUtils.saveChartAsPNG(CHART_FILE_PATH.toFile(), chart, data.getStageCount()*150, 1300);
    }

    public void setIncludeShadow(boolean includeShadow) { election.setIncludeShadow(includeShadow); }

    public boolean toggleIncludeShadow() { return election.toggleIncludeShadow(); }

    private void requireElection() {
        if (election == null) throw new IllegalStateException("Start an election first");
    }

    //endregion

    //region Voting

    /** @return the current vote cast by the given voter */
    @Nullable
    public Vote getVote(@NotNull String voterName) {
        requireElection();

        return election.getVotes(race, false)
                .stream()
                .filter(v -> v.voterName.equals(voterName))
                .findAny()
                .orElse(null);
    }

    /** Cast a vote */
    public void addVote(@NotNull Vote vote) {
        requireElection();
        vote = WeightedVote.fromVote(vote); // Force a weighting // FIXME don't force a weighting
        election.addVote(race, vote);
    }

    /** Cast a vote on behalf of the given voter for the given list of candidates, in decreasing order of preference */
    public void addVote(@NotNull String voterName, @Nullable List<Option> orderedChoices) {
        SimpleRankingVote vote = new SimpleRankingVote(voterName);
        if (orderedChoices == null) orderedChoices = new ArrayList<>();
        vote.select(orderedChoices);
        addVote(vote);
    }

    /** @see Session#addVote(String, List)  */
    public void addVote(@NotNull String voterName, String... gameStrings) {
        requireElection();

        List<Option> options = Arrays.stream(gameStrings)
                .map(this::interpret)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        addVote(voterName, options);
    }

    public void rate(@NotNull String voterName, @NotNull Option option, int rating) {
        requireElection();

        if (!race.options().contains(option)) {
            throw new IllegalArgumentException("Option not recognized");
        }

        Vote vote = getVote(voterName);
        if (vote == null || !(vote instanceof WeightedVote)) {
            vote = new WeightedVote(voterName);
        }
        ((WeightedVote) vote).rate(option, (double) rating);

        addVote(vote);
    }

    /** @return true iff the voter is now currently vetoing the given option after this returns */
    public boolean veto(@NotNull String voterName, @NotNull Option option) {
        requireElection();

        Vote vote = getVote(voterName);
        if (vote == null) {
            vote = new WeightedVote(voterName);
        }
        addVote(vote);

        return vote.vetoToggle(option);
    }

    /** Clear the voter's current vote in this election. This is irreversible. */
    public void clearCurrentVote(@NotNull String voterName) {
        requireElection();

        election.getVotes(race)
                .stream()
                .filter(v -> v.voterName.equals(voterName)) // vote and shadow vote
                .forEach(v -> election.removeVote(race, v));
    }

    //endregion

    //region Default Votes

    /**
     * Records the given voter's current vote in this election as their "default" vote.
     * A voter's default vote can be {@link Session#loadDefaultVote(String) loaded} for use
     * in future elections.
     * @throws IOException for errors during write of default vote file
     */
    public void saveDefaultVote(@NotNull String voterName) throws IOException {
        requireElection();

        Vote vote = getVote(voterName);

        replaceDefaultVote(voterName, vote);
    }

    /** Load the given voter's existing {@link Session#saveDefaultVote(String) default vote}. It replaces the voters current vote. */
    public void loadDefaultVote(@NotNull String voterName) throws IOException {
        requireElection();

        List<Vote> recordedVotes = DataUtils.deserializeFile(VOTES_FILE_PATH, DataUtils::deserializeVote);

        Vote vote = recordedVotes
                .stream()
                .filter(Objects::nonNull)
                .filter(v -> v.voterName.equals(voterName))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Vote not found for " + voterName));

        addVote(vote);
    }

    /** Clear the given voter's existing {@link Session#saveDefaultVote(String) default vote}. This is irreversible. */
    public void clearDefaultVote(@NotNull String voterName) throws IOException { replaceDefaultVote(voterName, null); }

    /** Replace the given voter's existing {@link Session#saveDefaultVote(String) default vote}. This is irreversible. */
    private void replaceDefaultVote(@NotNull String voterName, @Nullable Vote vote) throws IOException {
        List<Vote> recordedVotes = DataUtils.deserializeFile(VOTES_FILE_PATH, DataUtils::deserializeVote);
        recordedVotes.removeIf(v -> v.voterName.equals(voterName));

        if (vote != null) recordedVotes.add(vote);

        DataUtils.writeFile(VOTES_FILE_PATH, DataUtils.serializeItems(recordedVotes));
    }

    //endregion

    //region Unspent Votes

    /**
     * In consecutive elections, if some voter(s) always vote for unpopular options, their votes are "wasted".
     * In order to let them win occasionally, their preferences need to build in weight over time
     * according to how much of their vote they spent on eliminated options.
     * <br>
     * This method should be called <strong>only once</strong> after a winner is determined.
     * It records that "wasted" portion of votes so they can be heard in future elections.
     * This portion is calculated according to {@link WeightedVote#unspentWeight(Vote, Option)}.
     * @param winner The option that won the most recent {@link Race race}
     * @throws IOException for errors during read/write of unspent file
     */
    public void recordUnspentVotes(@NotNull Option winner) throws IOException {
        Set<WeightedVote> unspentVotes = election.getVotes(race, false)
                .stream()
                .map(v -> WeightedVote.unspentWeight(v, winner))
                .collect(Collectors.toSet());
        updateUnspentVotes(unspentVotes, winner);
    }

    @NotNull
    @Contract(" -> new")
    private Set<WeightedVote> loadUnspentVotes() throws IOException {
        return new HashSet<>(DataUtils.deserializeFile(UNSPENT_FILE_PATH, DataUtils::deserializeWeightedVote));
    }

    /**
     * @param updates Unspent vote weights from the current election, to be added to existing unspent votes
     * @param winner The winner of the current election
     * @throws IOException for errors during read/write of unspent file
     */
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

        DataUtils.writeFile(UNSPENT_FILE_PATH, DataUtils.serializeItems(recordedVotes));
    }

    //endregion

    //region Stored Candidates

    public void storeCandidates() throws IOException {
        requireElection();
        DataUtils.writeFile(SAVED_OPTIONS_FILE_PATH, DataUtils.serializeItems(getOptions()));
    }

    @NotNull
    @Contract(" -> new")
    private Set<Option> loadStoredCandidates() throws IOException {
        return new HashSet<>(DataUtils.deserializeFile(SAVED_OPTIONS_FILE_PATH, DataUtils::deserializeOption));
    }

    //endregion
}
