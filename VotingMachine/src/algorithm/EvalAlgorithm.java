package algorithm;

import model.Option;
import model.Race;
import model.Vote;

import java.util.Set;

public abstract class EvalAlgorithm {
    protected final Race race;

    public EvalAlgorithm(Race race) {
        this.race = race;
    }

    // return a set of tied winners
    public abstract Set<Option> evaluate(Set<Vote.RankedChoiceVote> votes);

    protected abstract void initializeStandings();
}
