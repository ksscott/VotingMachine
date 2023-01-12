package model;

import java.util.HashMap;
import java.util.Map;

public class SingleVote extends Vote {
    private Map<Race, Option> selections;

    public SingleVote(Ballot ballot, String voterName) {
        super(ballot, voterName);
        this.selections = new HashMap<>();
        this.ballot.races().forEach(r -> selections.put(r, null));
    }

    public void select(Race race, Option choice) {
        if (!this.ballot.races().contains(race)) {
            throw new IllegalArgumentException("Race doesn't appear on this ballot");
        }
        if (!race.options().contains(choice)) {
            throw new IllegalArgumentException("Invalid choice");
        }

        selections.put(race, choice);
    }

    public Option getVote(Race race) {
        if (!selections.containsKey(race)) {
            throw new IllegalArgumentException("Race not contained in vote");
        }

        return selections.get(race);
    }
}
