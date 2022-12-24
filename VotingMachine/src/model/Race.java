package model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Race {
	public final String name;
	private final Set<Option> options;
	
	public Race(String name, Set<Option> options) {
		this.name = name;
		this.options = new HashSet<>(options);
	}
	
	public Set<Option> getOptions() {
		return Collections.unmodifiableSet(options);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((options == null) ? 0 : options.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Race other = (Race) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (options == null) {
			if (other.options != null)
				return false;
		} else if (!options.equals(other.options))
			return false;
		return true;
	}
}
