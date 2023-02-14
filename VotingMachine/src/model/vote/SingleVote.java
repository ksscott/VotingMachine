package model.vote;

import model.Option;
import model.Race;

public class SingleVote extends Vote {
    private Option selection;

    public SingleVote(Race race, String voterName) { super(race, voterName); }

    @Override
    public SingleVote toSingleVote() { return this; }

    public void select(Option choice) {
        if (!race.options().contains(choice)) {
            throw new IllegalArgumentException("Invalid choice");
        }

        selection = choice;
    }

    public Option getVote() {
        return selection;
    }
}
