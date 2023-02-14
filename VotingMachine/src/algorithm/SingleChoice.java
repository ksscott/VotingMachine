package algorithm;

import model.Option;
import model.Race;
import model.vote.Vote;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SingleChoice extends EvalAlgorithm<Vote> {

    public SingleChoice(Race race) {
        super(race);
    }

    @Override
    public Set<Option> evaluate(Set<Vote> votes) {
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

        return winners;
    }
}
