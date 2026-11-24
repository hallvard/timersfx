package no.hal.timers.core;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.Optional;

/**
 * Class representing a participant taking part in a competition.
 * Keeps track of the participant's status and timings.
 */
public record Participation(Participant participant, Timings<String, LocalTime> timings) {

  /**
   * Initializes with the participant.
   *
   * @param participant the participant
   */

   public Participation(Participant participant) {
    this(participant, new Timings<String, LocalTime>());
  }

  /**
   * The possible statuses for a participation.
   */
  public static enum Status {
    START, ACTIVE, FINISH, STOP;
  }

  /**
   * Checks if the participation is for a given participant.
   *
   * @param participant the participant to check
   * @return true if the participation is for the participant
   */
  public boolean isFor(Participant participant) {
    return this.participant == participant;
  }

  public Participant getParticipant() {
    return participant;
  }

  public Optional<LocalTime> getStartTime() {
    return Optional.ofNullable(timings.getStartTime());
  }

  /**
   * Starts the participation at the given time.
   *
   * @param time the time to start
   */
  public void start(LocalTime time) {
    timings.setStartTime(time);
  }

  /**
   * Stops the participation at the given time.
   *
   * @param time the time to stop
   */
  public void stop(LocalTime time) {
    time(time, Status.STOP);
  }

  /**
   * Finishes the participation at the given time.
   *
   * @param time the time to finish
   */
  public void finish(LocalTime time) {
    time(time, Status.FINISH);
  }

  /**
   * Sets the time for a given status.
   *
   * @param time the duration after the start time
   * @param status the status
   */
  public void time(Duration time, Status status) {
    time(time, status.name());
  }
  
  /**
   * Sets the time for a given key.
   *
   * @param time the duration after the start time
   * @param key the key
   */
  public void time(Duration time, String key) {
    timings.setTime(time, key);
  }

  /**
   * Sets the time for a given status.
   *
   * @param time the time
   * @param status the status
   */
  public void time(LocalTime time, Status status) {
    time(time, status.name());
  }
  
  /**
   * Sets the time for a given key.
   *
   * @param time the time
   * @param key the key
   */
  public void time(LocalTime time, String key) {
    timings.setTime(time, key);
  }

  /**
   * Gets the status of the participation,
   * based on recorded timings.
   *
   * @return the status
   */
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
    return Status.START;
  }

  /**
   * Gets the duration for a given key.
   *
   * @param key the key
   * @return the duration, wrapped in an Optional
   */
  public Optional<Duration> getDuration(String key) {
    return timings.getDuration(key);
  }

  /**
   * Checks if the participation has a time for a given key.
   *
   * @param key the key
   * @return true if the participation has a time for the key
   */
  public boolean hasTime(String key) {
    return timings.hasTime(key);
  }

  /**
   * Gets the time for a given key.
   *
   * @param key the key
   * @return the time, wrapped in an Optional
   */
  public Optional<Temporal> getTime(String key) {
    return timings.getTime(key);
  }
}
