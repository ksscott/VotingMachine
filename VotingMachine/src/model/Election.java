package model;

import model.vote.Vote;

import java.util.*;

public class Election<V extends Vote> {

	public final Ballot ballot;
	private Map<Race, Set<V>> votes;

	public Election(Ballot ballot) {
		this.ballot = ballot;
		this.votes = new HashMap<>();
		for (Race race : ballot.races()) {
			votes.put(race, new HashSet<>());
		}
	}

	public void addVote(Race race, V vote) {
		if (!ballot.races().contains(race)) {
			throw new IllegalArgumentException("Race does not appear on this ballot");
		}
		this.votes.get(race).remove(vote);
		this.votes.get(race).add(vote);
	}

	public Set<V> getVotes(Race race) {
		if (!ballot.races().contains(race)) {
			throw new IllegalArgumentException("Race does not appear on this ballot");
		}
		return Collections.unmodifiableSet(votes.get(race));
	}

	public boolean removeVote(Race race, V vote) {
		if (!ballot.races().contains(race)) {
			throw new IllegalArgumentException("Race does not appear on this ballot");
		}
		return votes.get(race).remove(vote);
	}
}
