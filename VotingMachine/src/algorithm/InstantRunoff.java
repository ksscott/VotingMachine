package algorithm;

import model.Option;
import model.Race;
import model.vote.RankedChoiceVote;

import java.util.*;

public class InstantRunoff extends EvalAlgorithm<RankedChoiceVote> {
    // WARNING There's a corner case this algorithm doesn't account for
    // where one person votes 1)A,2)B and the other votes 1)B,2)A

    private Map<Option, Set<RankedChoiceVote>> standings;
    private Set<RankedChoiceVote> unassignedVoters;

    public InstantRunoff(Race race) {
        super(race);
    }

    // return a set of tied winners
    @Override
    public Set<Option> evaluate(Set<RankedChoiceVote> votes) {
        initializeStandings();
        System.out.println("Initializing standings...");

        this.unassignedVoters = votes;

        Set<Option> winners = null;
        while (winners == null) {
            System.out.println("Evaluating round...");
            winners = evaluateRound();
        }

        return winners;
    }

    protected void initializeStandings() {
        this.standings = new HashMap<>();
        race.options().forEach(option -> standings.put(option, new HashSet<>()));
    }

    /**
     * @return winners, if they have yet been found, null otherwise
     */
    private Set<Option> evaluateRound() {
        // assign unassigned voters
        caucus();

        // if strict majority, return winner
        Option strictWinner = strictWinner();
        if (strictWinner != null) {
            Set<Option> winningSet = new HashSet<>();
            winningSet.add(strictWinner);
            return winningSet;
        }

        // find candidate(s) with least votes
        Set<Option> losingCandidates = losers();

        // if no losers, return all winners
        if (losingCandidates == null) {
            return standings.keySet();
        }

        // WARNING: if there's a tie for loser, this removes ALL losers
        for (Option loser : losingCandidates) {
//            System.out.println("Removing loser: " + loser.name);
            // unassign their voters
            unassignedVoters.addAll(standings.get(loser));
            // drop the candidate
            standings.remove(loser);
        }

        return null; // loop again
    }

    // assign unassigned voters
    private void caucus() {
        for (RankedChoiceVote vote : unassignedVoters) {
//            System.out.println("Assigning a voter: " + vote.voterName);
            // assign vote
            List<Option> choices = vote.getVote();
//            System.out.println("Selections: " + choices);
            for (Option option : choices) {
//                System.out.println("Voted for: " + option.name);
                if (standings.containsKey(option)) {
                    standings.get(option).add(vote);
//                    System.out.println("Adding vote to " + option.name);
                    break;
                }
            }
        }
        unassignedVoters = new HashSet<>();
    }

    private Option strictWinner() {
        int remainingVoterCount = (int) standings.values().stream().mapToLong(Set::size).sum();

        for (Option candidate : standings.keySet()) {
            if (standings.get(candidate).size() > remainingVoterCount / 2) {
                return candidate;
            }
        }
        return null;
    }

    private Set<Option> losers() {
        Map<Option, Integer> count = new HashMap<>();
        race.options().forEach(option -> count.put(option, 0));

        for (Option candidate : standings.keySet()) {
            count.put(candidate, standings.get(candidate).size());
        }

        Set<Option> losers = new HashSet<>();
        int lowest = Integer.MAX_VALUE;
        for (Option option : count.keySet()) {
            int number = count.get(option);
            if (number < lowest) {
                losers = new HashSet<>();
                losers.add(option);
                lowest = number;
            } else if (number == lowest) {
                losers.add(option);
            }
        }

        if (losers.size() == count.keySet().size()) {
            return null; // all winners, no losers
        }

        return losers;
    }
}
