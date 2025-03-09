package no.hal.timers.core;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class Competition {

  // timing keys

  private List<String> timingKeys = null;

  public List<String> getTimingKeys() {
    return new ArrayList<String>(timingKeys);
  }

  // participations

  private final List<Participation> participations = new ArrayList<Participation>();

  public Iterator<Participation> participations() {
    return participations.iterator();
  }

  public int getParticipationCount() {
    return participations.size();
  }

  public Participation getParticipation(final int num) {
    return participations.get(num);
  }

  public Optional<Participation> getParticipation(final Participant participant) {
    return participations.stream()
        .filter(participation -> participation.isFor(participant))
        .findFirst();
  }

  public void setTimingKeys(final String... keys) {
    timingKeys = Arrays.asList(keys);
  }  

  public boolean hasParticipant(final Participant participant) {
    return participations.stream()
        .anyMatch(participation -> participation.isFor(participant));
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
    getParticipation(participant)
        .ifPresent(participation -> participation.setCompetition(null));
  }

  // time

  private TimeProvider timeProvider = TimeProvider.SYSTEM_TIME_PROVIDER;

  public void setTimeProvider(final TimeProvider timeProvider) {
    this.timeProvider = timeProvider;
  }

  public LocalTime getCurrentTime() {
    return timeProvider.get();
  }

  public Optional<LocalTime> getCurrentDuration(final Participation participation) {
    final Optional<LocalTime> startTime = participation.getStartTime();
    if (startTime.isPresent()) {
      Duration.between(timeProvider.get(), startTime.get());
    }
    return Optional.empty();
  }
}
