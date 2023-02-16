package model.vote;

import model.Option;

import java.util.*;

public class SimpleRankingVote extends RankedVote {
    private List<Option> selections;

    public SimpleRankingVote(String voterName) {
        super(voterName);
        this.selections = new ArrayList<>();
    }

    @Override
    public List<Option> getRankings() { return new ArrayList<>(selections); }

    public void select(List<Option> choices) {
        Set<Option> set = new HashSet<>(choices);
        if (set.size() != choices.size()) {
            throw new IllegalArgumentException("Choices must be unique");
        }

        selections = choices;
    }

    public static SimpleRankingVote fromVote(Vote vote) {
        SimpleRankingVote srv = new SimpleRankingVote(vote.voterName);
        if (vote instanceof RankedVote rv) {
            srv.select(rv.getRankings());
        } else {
           srv.select(Collections.singletonList(vote.toSingleVote().getVote()));
        }
        return srv;
    }
}
