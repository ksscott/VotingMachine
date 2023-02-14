package algorithm;

import model.Option;
import model.Race;
import model.vote.RankedVote;

import java.util.*;
import java.util.stream.Collectors;

public class InstantRunoff extends EvalAlgorithm<RankedVote> {
    // WARNING There's a corner case this algorithm doesn't account for
    // where one person votes 1)A,2)B and the other votes 1)B,2)A

    private Map<Option, Set<RankedVote>> standings;
    private Set<RankedVote> unassignedVoters;

    public InstantRunoff(Race race) {
        super(race);
    }

    // return a set of tied winners
    @Override
    public Set<Option> evaluate(Set<RankedVote> votes) {
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
        for (RankedVote vote : unassignedVoters) {
//            System.out.println("Assigning a voter: " + vote.voterName);
            // assign vote
            List<Option> choices = vote.getRankings();
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

        return standings.keySet()
                .stream()
                .filter(candidate -> standings.get(candidate).size() > remainingVoterCount / 2)
                .findAny()
                .orElse(null);
    }

    private Set<Option> losers() {
        double lowestScore = standings.values()
                .stream()
                .mapToInt(Set::size)
                .reduce(Integer::min)
                .orElse(Integer.MAX_VALUE);
        Set<Option> losers = standings.keySet()
                .stream()
                .filter(candidate -> standings.get(candidate).size() <= lowestScore)
                .collect(Collectors.toSet());
        // check for: all winners, no losers
        return losers.size() == standings.size() ? null : losers;
    }
}
