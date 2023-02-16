package model.vote;

import com.fasterxml.jackson.annotation.JsonIgnore;
import model.Option;

import java.util.List;

public abstract class RankedVote extends Vote {

    public RankedVote(String voterName) { super(voterName); }

    @JsonIgnore
    public abstract List<Option> getRankings();

    @Override
    public SingleVote toSingleVote() {
        SingleVote vote = new SingleVote(this.voterName);
        if (!getRankings().isEmpty()) {
            vote.select(getRankings().get(0));
        }
        return vote;
    }
}
