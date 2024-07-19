package model;

import model.vote.Vote;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Election<V extends Vote> {

	private Ballot ballot;
	private Map<Race, Set<V>> votes;
	private boolean includeShadow;

	public Election(Ballot ballot) {
		this.ballot = ballot;
		this.votes = new HashMap<>();
		this.includeShadow = true;
		for (Race race : ballot.races()) {
			votes.put(race, new HashSet<>());
		}
	}

	public Ballot getBallot() { return this.ballot; }

	public void updateRace(@NotNull Race race, @NotNull Race newRace) {
		Set<V> oldVotes = votes.get(race);
		votes.remove(race);
		votes.put(newRace, oldVotes);

		Set<Race> oldRaces = new HashSet<>(ballot.races());
		if (!oldRaces.remove(race)) {
			throw new IllegalStateException("Ran into troubles tracking electoral races");
		}
		oldRaces.add(newRace);
		ballot = new Ballot(ballot.name(), oldRaces);
	}

	public void addVote(Race race, V vote) {
		requireRace(race);
		this.votes.get(race).remove(vote);
		this.votes.get(race).add(vote);
	}

	public Set<V> getVotes(Race race) {
		return getVotes(race, this.includeShadow);
	}
	public Set<V> getVotes(Race race, boolean includingShadow) {
		requireRace(race);
		return votes.get(race)
				.stream()
				.filter(v -> (includingShadow || !v.isShadow()))
				.collect(Collectors.toSet());
	}

	public boolean removeVote(Race race, V vote) {
		requireRace(race);
		return votes.get(race).remove(vote);
	}

	public boolean setIncludeShadow(boolean includeShadow) {
		this.includeShadow = includeShadow;
		return this.includeShadow;
	}

	public boolean toggleIncludeShadow() {
		includeShadow = !this.includeShadow;
		return includeShadow;
	}

	private void requireRace(Race race) {
		if (!ballot.races().contains(race)) {
			throw new IllegalArgumentException("Race does not appear on this ballot");
		}
	}

}
