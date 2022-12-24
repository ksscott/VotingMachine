package model;

import java.util.Collections;
import java.util.Set;

public class Ballot {
	public final String name;
	private Set<Race> races;
	
	public Ballot(String name, Set<Race> races) {
		this.name = name;
		this.races = races;
	}
	
	public Set<Race> getRaces() {
		return Collections.unmodifiableSet(races);
	}
}
