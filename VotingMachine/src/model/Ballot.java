package model;

import java.util.*;

public record Ballot(String name, Set<Race> races) {

	public Ballot(String name, Race... races) {
		this(name, Set.of(races));
	}
}
