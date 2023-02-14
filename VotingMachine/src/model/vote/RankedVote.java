package model.vote;

import model.Option;
import model.Race;

import java.util.List;

public abstract class RankedVote extends Vote {

    public RankedVote(Race race, String voterName) { super(race, voterName); }

    public abstract List<Option> getRankings();

    public SingleVote toSingleVote() {
        SingleVote vote = new SingleVote(this.race, this.voterName);
        if (!getRankings().isEmpty()) {
            vote.select(getRankings().get(0));
        }
        return vote;
    }
}
