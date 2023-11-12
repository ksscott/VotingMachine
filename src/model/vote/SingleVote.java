package model.vote;

import model.Option;

public class SingleVote extends Vote {
    private Option selection;

    public SingleVote(String voterName) { super(voterName); }

    @Override
    public SingleVote toSingleVote() { return this; }

    public void select(Option choice) { selection = choice; }

    public Option getVote() { return selection; }
}
