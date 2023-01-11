package model;

import java.util.*;

public abstract class Vote {
	public final Ballot ballot;
	public final String voterName;
	
	public Vote(Ballot ballot, String voterName) {
		this.ballot = ballot;
		this.voterName = voterName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vote vote)) return false;
		return Objects.equals(ballot, vote.ballot) && Objects.equals(voterName, vote.voterName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ballot, voterName);
	}

	public static class SingleVote extends Vote {
		private Map<Race,Option> selections;

		public SingleVote(Ballot ballot, String voterName) {
			super(ballot, voterName);
			this.selections = new HashMap<>();
			this.ballot.getRaces().forEach(r -> selections.put(r, null));
		}
		
		public SingleVote(RankedChoiceVote vote) {
			super(vote.ballot, vote.voterName);
			this.selections = new HashMap<>();
			for (Race race : vote.selections.keySet()) {
				Optional<Option> pick = Optional.of(vote.selections.get(race).get(0));
				this.selections.put(race, pick.orElse(null));
			}
		}
		
		public void select(Race race, Option choice) {
			if (!this.ballot.getRaces().contains(race)) {
				throw new IllegalArgumentException("Race doesn't appear on this ballot");
			}
			if (!race.getOptions().contains(choice)) {
				throw new IllegalArgumentException("Invalid choice");
			}
			
			selections.put(race, choice);
		}
		
		public Option getVote(Race race) {
			if (!selections.containsKey(race)) {
				throw new IllegalArgumentException("Race not contained in vote");
			}
			
			return selections.get(race);
		}
	}
	
	public static class RankedChoiceVote extends Vote {
		private Map<Race,List<Option>> selections;
		
		public RankedChoiceVote(Ballot ballot, String voterName) {
			super(ballot, voterName);
			this.selections = new HashMap<>();
		}
		
		public void select(Race race, List<Option> choices) {
			if (!this.ballot.getRaces().contains(race)) {
				throw new IllegalArgumentException("Race doesn't appear on this ballot");
			}
			Set<Option> set = new HashSet<>(choices);
			if (set.size() != choices.size()) {
				throw new IllegalArgumentException("Choices must be unique");
			}
			if (!race.getOptions().containsAll(set)) {
				throw new IllegalArgumentException("Option does not appear in this race");
			}
			
			selections.put(race, choices);
		}
		
		public List<Option> getVote(Race race) {
			return selections.get(race);
		}
	}
}
