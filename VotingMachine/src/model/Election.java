package model;

import model.vote.Vote;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Election<V extends Vote> {

	public final Ballot ballot;
	private Map<Race, Set<V>> votes;
	private boolean includeShadow = true;

	public Election(Ballot ballot) {
		this.ballot = ballot;
		this.votes = new HashMap<>();
		for (Race race : ballot.races()) {
			votes.put(race, new HashSet<>());
		}
	}

	public void addVote(Race race, V vote) {
		requireRace(race);
		this.votes.get(race).remove(vote);
		this.votes.get(race).add(vote);
		System.out.println("Added a vote");
		System.out.println(votes.get(race));
	}

	public Set<V> getVotes(Race race) {
		return getVotes(race, this.includeShadow);
	}
	public Set<V> getVotes(Race race, boolean includingShadow) {
		requireRace(race);
		System.out.println("Returning votes");
		System.out.println(votes);
		return votes.get(race)
				.stream()
				.peek(v -> System.out.println("Trying: " + v))
				.filter(v -> (includingShadow || !v.isShadow()))
				.peek(v -> System.out.println("Including: " + v))
				.collect(Collectors.toSet());
	}

	public boolean removeVote(Race race, V vote) {
		requireRace(race);
		System.out.println("Removing vote: " + vote);
		System.out.println("Existing votes: " + votes);
		return votes.get(race).remove(vote);
	}

	public void setIncludeShadow(boolean includeShadow) { this.includeShadow = includeShadow; }

	private void requireRace(Race race) {
		if (!ballot.races().contains(race)) {
			throw new IllegalArgumentException("Race does not appear on this ballot");
		}
	}
}
