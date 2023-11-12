package model;

import java.util.Set;

public record Ballot(String name, Set<Race> races) {

	public Ballot(String name, Race... races) {
		this(name, Set.of(races));
	}
}
