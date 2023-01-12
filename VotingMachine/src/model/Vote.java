package model;

import java.util.*;

public abstract class Vote {
	public final Ballot ballot;
	public final String voterName;
	
	public Vote(Ballot ballot, String voterName) {
		this.ballot = ballot;
		this.voterName = voterName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vote vote)) return false;
		return Objects.equals(ballot, vote.ballot) && Objects.equals(voterName, vote.voterName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ballot, voterName);
	}
}
