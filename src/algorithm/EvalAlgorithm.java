package algorithm;

import model.Race;
import model.Result;
import model.vote.Vote;

import java.util.Set;

public abstract class EvalAlgorithm<V extends Vote> {
    protected final Race race;

    public EvalAlgorithm(Race race) {
        this.race = race;
    }

    // return a set of tied winners
    public abstract Result evaluate(Set<V> votes);
}
