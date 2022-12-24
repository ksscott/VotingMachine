package model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Election<V extends Vote> {
	public final Ballot ballot;
	private Set<V> votes;
	
	public Election(Ballot ballot) {
		this.ballot = ballot;
		this.votes = new HashSet<>();
	}
	
	public void addVote(V vote) {
		this.votes.add(vote);
	}
	
	public Set<V> getVotes() {
		return Collections.unmodifiableSet(votes);
	}
}
