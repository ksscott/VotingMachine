package model;

import java.util.*;

public class RankedChoiceVote extends Vote {
    private Map<Race, List<Option>> selections;

    public RankedChoiceVote(Ballot ballot, String voterName) {
        super(ballot, voterName);
        this.selections = new HashMap<>();
    }

    public void select(Race race, List<Option> choices) {
        if (!this.ballot.races().contains(race)) {
            throw new IllegalArgumentException("Race doesn't appear on this ballot");
        }
        Set<Option> set = new HashSet<>(choices);
        if (set.size() != choices.size()) {
            throw new IllegalArgumentException("Choices must be unique");
        }
        if (!race.options().containsAll(set)) {
            throw new IllegalArgumentException("Option does not appear in this race");
        }

        selections.put(race, choices);
    }

    public List<Option> getVote(Race race) {
        return selections.get(race);
    }

    public SingleVote toSingleVote() {
        SingleVote vote = new SingleVote(this.ballot, this.voterName);
        for (Race race : selections.keySet()) {
            vote.select(race, selections.get(race).get(0));
        }
        return vote;
    }
}
