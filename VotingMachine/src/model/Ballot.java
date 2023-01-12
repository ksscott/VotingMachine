package model;

import org.apache.commons.collections4.set.UnmodifiableSet;

import java.util.*;

public record Ballot(String name, UnmodifiableSet<Race> races) {

	public Ballot(String name, Race... races) {
		this(name, (UnmodifiableSet<Race>) Set.of(races));
	}
}
