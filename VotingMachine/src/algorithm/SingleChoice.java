package algorithm;

import model.Option;
import model.Race;
import model.vote.Vote;
import model.EvaluationResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import org.javatuples.*;

public class SingleChoice extends EvalAlgorithm<Vote> {

    public SingleChoice(Race race) {
        super(race);
    }

    @Override
    public List<Triplet<EvaluationResult,Double,Set<Option>>> evaluate(Set<Vote> votes) {
        Map<Option, Integer> count = new HashMap<>();
        race.options().forEach(option -> count.put(option, 0));

        for (Vote vote : votes) {
            Option choice = vote.toSingleVote().getVote();
            if (choice != null) {
                count.put(choice, count.get(choice)+1);
            }
        }

        Set<Option> winners = new HashSet<>();
        int highest = 0;
        for (Option option : count.keySet()) {
            int number = count.get(option);
            if (number > highest) {
                winners = new HashSet<>();
                winners.add(option);
                highest = number;
            } else if (number == highest) {
                winners.add(option);
            }
        }

        List<Triplet<EvaluationResult,Double,Set<Option>>> roundresults = new ArrayList<Triplet<EvaluationResult,Double,Set<Option>>>();
        roundresults.add(Triplet.with(EvaluationResult.WINNERS, 0.0, winners));
        
        return roundresults;
    }
}
