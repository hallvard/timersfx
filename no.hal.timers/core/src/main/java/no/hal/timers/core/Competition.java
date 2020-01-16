package no.hal.timers.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class Competition {

	private final List<Participation> participations;

	private List<String> timingKeys = null;

	public Competition() {
		participations = new ArrayList<Participation>();
	}

	public List<String> getTimingKeys() {
		return new ArrayList<String>(timingKeys);
	}

	public Iterator<Participation> participations() {
		return participations.iterator();
	}

	public int getParticipationCount() {
		return participations.size();
	}

	public Participation getParticipation(final int num) {
		return participations.get(num);
	}

	public void setTimingKeys(final String... keys) {
		timingKeys = Arrays.asList(keys);
	}

	public boolean hasParticipant(final Participant participant) {
		return participations.stream().anyMatch(participation -> participation.isFor(participant));
	}

	public Optional<Participation> getParticipation(final Participant participant) {
		return participations.stream().filter(participation -> participation.isFor(participant)).findFirst();
	}

	void addParticipation(final Participation participation) {
		if (! participations.contains(participation)) {
			participations.add(participation);
		}
	}

	public Participation addParticipant(final Participant participant) {
		if (! hasParticipant(participant)) {
			// will call addParticipation
			return new Participation(participant, this);
		}
		return null;
	}

	void removeParticipation(final Participation participation) {
		participations.remove(participation);
	}

	public void removeParticipant(final Participant participant) {
		getParticipation(participant).ifPresent(participation -> participation.setCompetition(null));
	}
}
