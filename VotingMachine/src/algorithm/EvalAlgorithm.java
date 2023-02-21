package algorithm;

import model.Option;
import model.Race;
import model.vote.Vote;

import java.util.Set;
import java.util.List;

import model.EvaluationResult;

import org.javatuples.*;

public abstract class EvalAlgorithm<V extends Vote> {
    protected final Race race;

    public EvalAlgorithm(Race race) {
        this.race = race;
    }

    // return a set of tied winners
    public abstract List<Triplet<EvaluationResult,Double,Set<Option>>> evaluate(Set<V> votes);
}
