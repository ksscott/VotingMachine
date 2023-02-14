package model.vote;

import model.Option;
import model.Race;

import java.util.*;

public class SimpleRankingVote extends RankedVote {
    private List<Option> selections;

    public SimpleRankingVote(Race race, String voterName) {
        super(race, voterName);
        this.selections = new ArrayList<>();
    }

    @Override
    public List<Option> getRankings() { return new ArrayList<>(selections); }

    public void select(List<Option> choices) {
        Set<Option> set = new HashSet<>(choices);
        if (set.size() != choices.size()) {
            throw new IllegalArgumentException("Choices must be unique");
        }
        if (!race.options().containsAll(set)) {
            throw new IllegalArgumentException("Option(s) do not appear in this race");
        }

        selections = choices;
    }
}
