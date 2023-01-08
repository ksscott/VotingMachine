package model;

import java.util.Collections;
import java.util.HashSet;
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
}
