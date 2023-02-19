package model.vote;

import model.Option;

import java.io.Serializable;
import java.util.*;

public abstract class Vote implements Serializable {

	public final String voterName;
	private Set<Option> vetoes;
	
	public Vote(String voterName) {
		this.voterName = voterName;
		this.vetoes = new HashSet<>();
	}

	public abstract SingleVote toSingleVote();

	public void veto(Option option) { vetoes.add(option); }

	public Set<Option> getVetoes() { return Collections.unmodifiableSet(vetoes); }

	public void clearVetoes() { this.vetoes = new HashSet<>(); }

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
