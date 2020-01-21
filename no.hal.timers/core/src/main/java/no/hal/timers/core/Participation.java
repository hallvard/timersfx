package no.hal.timers.core;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.Optional;

public class Participation {

	public static enum Status {
		INACTIVE, START, ACTIVE, FINISH, STOP;
	}

	private final Participant participant;
	private Competition competition = null;

	public Participation(final Participant participant, final Competition competition) {
		this.participant = participant;
		setCompetition(competition);
	}

	public Participation(final Participant participant) {
		this(participant, null);
	}

	public Competition getCompetition() {
		return competition;
	}

	public void setCompetition(final Competition competition) {
		if (this.competition != null) {
			this.competition.removeParticipation(this);
		}
		this.competition = competition;
		if (this.competition != null) {
			this.competition.addParticipation(this);
		}
	}

	private Timings<String, LocalTime> timings;

	public boolean isFor(final Participant participant) {
		return this.participant == participant;
	}

	public Participant getParticipant() {
		return participant;
	}

	public Optional<LocalTime> getStartTime() {
		return (timings != null ? Optional.of(timings.getStartTime()) : Optional.empty());
	}

	public void start(final LocalTime time) {
		ensureTimings();
		timings.setStartTime(time);
	}

	private void ensureTimings() {
		if (timings == null) {
			timings = new Timings<String, LocalTime>();
		}
	}

	public void stop(final LocalTime time) {
		time(time, Status.STOP);
	}

	public void finish(final LocalTime time) {
		time(time, Status.FINISH);
	}

	protected void time(final Duration time, final Status status) {
		time(time, status.name());
	}
	public void time(final Duration time, final String key) {
		ensureTimings();
		timings.setTime(time, key);
	}

	protected void time(final LocalTime time, final Status status) {
		time(time, status.name());
	}
	public void time(final LocalTime time, final String key) {
		ensureTimings();
		timings.setTime(time, key);
	}

	public Status getStatus() {
		if (timings != null) {
			if (timings.hasTime(Status.STOP.name())) {
				return Status.STOP;
			} else if (timings.hasTime(Status.FINISH.name())) {
				return Status.FINISH;
			} else if (timings.getStartTime() != null) {
				return Status.ACTIVE;
			}
		}
		return (competition != null ? Status.START : Status.INACTIVE);
	}

	public Optional<Duration> getDuration(final String key) {
		return (timings != null ? timings.getDuration(key) : Optional.empty());
	}

	public boolean hasTime(final String key) {
		return timings != null && timings.hasTime(key);
	}

	public Optional<Temporal> getTime(final String key) {
		return (timings != null ? timings.getTime(key) : Optional.empty());
	}
}
