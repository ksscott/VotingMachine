package algorithm;

import model.Option;
import model.Race;
import model.Result;
import model.vote.SingleVote;
import model.vote.Vote;

import java.util.*;

public class SingleChoice extends EvalAlgorithm<Vote> {

    public SingleChoice(Race race) {
        super(race);
    }

    @Override
    public Result evaluate(Set<Vote> votes) {
        Map<Option, Integer> count = new HashMap<>();
        race.options().forEach(option -> count.put(option, 0));

        votes.stream()
                .map(Vote::toSingleVote)
                .map(SingleVote::getVote)
                .filter(Objects::nonNull)
                .forEach(o -> count.put(o, count.get(o)+1));
        votes.stream()
                .map(Vote::getVetoes)
                .flatMap(Set::stream)
                .forEach(count::remove);

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

        return new Result(winners, null);
    }
}
