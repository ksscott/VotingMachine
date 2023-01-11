package model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Ballot {
	public final String name;
	private Set<Race> races;
	
	public Ballot(String name, Set<Race> races) {
		this.name = name;
		this.races = races;
	}

	public Ballot(String name, Race... races) {
		this(name, new HashSet<>());
		for (Race race : races) {
			this.races.add(race);
		}
	}
	
	public Set<Race> getRaces() {
		return Collections.unmodifiableSet(races);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Ballot ballot = (Ballot) o;
		return Objects.equals(name, ballot.name) && Objects.equals(races, ballot.races);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, races);
	}
}
