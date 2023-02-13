package model.vote;

import model.Race;

import java.util.*;

public abstract class Vote {
	public final Race race;
	public final String voterName;
	
	public Vote(Race race, String voterName) {
		this.race = race;
		this.voterName = voterName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vote vote)) return false;
		return Objects.equals(race, vote.race) && Objects.equals(voterName, vote.voterName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(race, voterName);
	}
}
