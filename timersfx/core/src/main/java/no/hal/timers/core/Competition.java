package no.hal.timers.core;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * A competition, the main container class.
 */
public class Competition {

  // timing keys

  private List<String> timingKeys = null;

  public List<String> getTimingKeys() {
    return new ArrayList<String>(timingKeys);
  }

  // participations

  private final List<Participation> participations = new ArrayList<Participation>();

  /**
   * An iterator over the participations.
   *
   * @return an iterator over the participations
   */
  public Iterator<Participation> participations() {
    return participations.iterator();
  }

  public int getParticipationCount() {
    return participations.size();
  }

  /**
   * Get a participation by index.
   *
   * @param num the index
   * @return the participation at the index
   */
  public Participation getParticipation(int num) {
    return participations.get(num);
  }

  /**
   * Get a participation for a participant.
   *
   * @param participant the participant
   * @return the participation for the participant, wrapped in an Optional
   */
  public Optional<Participation> getParticipation(Participant participant) {
    return participations.stream()
        .filter(participation -> participation.isFor(participant))
        .findFirst();
  }

  public void setTimingKeys(String... keys) {
    timingKeys = List.of(keys);
  }  

  /**
   * Check if a participant is in the competition.
   *
   * @param participant the participant to check
   * @return true if the participant is in the competition
   */
  public boolean hasParticipant(Participant participant) {
    return participations.stream()
        .anyMatch(participation -> participation.isFor(participant));
  }  

  void addParticipation(Participation participation) {
    if (! participations.contains(participation)) {
      participations.add(participation);
    }
  }

  /**
   * Add a participant to the competition, if not already present.
   *
   * @param participant the participant to add
   * @return the participation added, or null if the participant was already present
   */
  public Participation addParticipant(Participant participant) {
    if (! hasParticipant(participant)) {
      // will call addParticipation
      return new Participation(participant, this);
    }
    return null;
  }

  void removeParticipation(Participation participation) {
    participations.remove(participation);
  }

  /**
   * Remove a participant from the competition.
   *
   * @param participant the participant to remove
   */
  public void removeParticipant(Participant participant) {
    getParticipation(participant)
        .ifPresent(participation -> participation.setCompetition(null));
  }

  // time

  private TimeProvider timeProvider = TimeProvider.SYSTEM_TIME_PROVIDER;

  public void setTimeProvider(TimeProvider timeProvider) {
    this.timeProvider = timeProvider;
  }

  public LocalTime getCurrentTime() {
    return timeProvider.get();
  }

  /**
   * Get the current duration for a participation, if it has started.
   *
   * @param participation the participation
   * @return the current duration, wrapped in an Optional
   */
  public Optional<Duration> getCurrentDuration(Participation participation) {
    return participation.getStartTime()
      .map(startTime -> Duration.between(timeProvider.get(), startTime));
  }
}
