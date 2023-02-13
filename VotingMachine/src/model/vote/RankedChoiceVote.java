package model.vote;

import model.Option;
import model.Race;

import java.util.*;

public class RankedChoiceVote extends Vote {
    private List<Option> selections;

    public RankedChoiceVote(Race race, String voterName) {
        super(race, voterName);
        this.selections = new ArrayList<>();
    }

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

    public List<Option> getVote() {
        return new ArrayList<>(selections);
    }

    public SingleVote toSingleVote() {
        SingleVote vote = new SingleVote(this.race, this.voterName);
        vote.select(selections.get(0));
        return vote;
    }
}
