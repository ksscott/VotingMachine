package algorithm;

import model.Option;
import model.Race;
import model.Vote;

import java.util.Set;

public abstract class EvalAlgorithm<V extends Vote> {
    protected final Race race;

    public EvalAlgorithm(Race race) {
        this.race = race;
    }

    // return a set of tied winners
    public abstract Set<Option> evaluate(Set<V> votes);
}
