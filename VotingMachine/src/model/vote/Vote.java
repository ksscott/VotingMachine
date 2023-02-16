package model.vote;

import java.io.Serializable;
import java.util.*;

public abstract class Vote implements Serializable {

	public final String voterName;
	
	public Vote(String voterName) {
		this.voterName = voterName;
	}

	public abstract SingleVote toSingleVote();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vote vote)) return false;
		return Objects.equals(voterName, vote.voterName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(voterName);
	}
}
