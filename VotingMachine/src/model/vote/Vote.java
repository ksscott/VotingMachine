package model.vote;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.Option;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class Vote implements Serializable {

	public final String voterName;
	protected Set<Option> vetoes;
	@JsonProperty
	protected boolean shadow;

	public Vote(String voterName) {
		this.voterName = voterName;
		this.vetoes = new HashSet<>();
	}

	public abstract SingleVote toSingleVote();

	public void veto(Option option) { vetoes.add(option); }

	public Set<Option> getVetoes() { return Collections.unmodifiableSet(vetoes); }

	public void clearVetoes() { this.vetoes = new HashSet<>(); }

	public boolean isShadow() { return this.shadow; }

	public void setShadow(boolean shadow) { this.shadow = shadow; }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vote vote)) return false;
		return shadow == vote.shadow && Objects.equals(voterName, vote.voterName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(voterName, shadow);
	}
}
