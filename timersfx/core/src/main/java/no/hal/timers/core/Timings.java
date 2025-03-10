package no.hal.timers.core;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * A set of timings relative to a start time.
 */
public class Timings<K, T extends Temporal & Comparable<T>> {

  private T startTime = null;
  private List<Map.Entry<K, Duration>> timings;

  public int getCount() {
    return (timings != null ? timings.size() : 0);
  }

  /**
   * Initializes without a start time.
   */
  public Timings() {
  }

  /**
   * Initializes with a start time.
   *
   * @param time the start time
   */
  public Timings(T time) {
    setStartTime(time);
  }

  public T getStartTime() {
    return startTime;
  }

  public void setStartTime(T startTime) {
    this.startTime = startTime;
  }

  private int timePos(K key, int start) {
    if (timings != null) {
      for (var i = start; i < timings.size(); i++) {
        if (timings.get(i).getKey().equals(key)) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Check if a key has a time.
   *
   * @param key the key
   * @return true if the key has a time
   */
  public boolean hasTime(K key) {
    return timePos(key, 0) >= 0;
  }

  /**
   * Applies a function to the time of a key.
   *
   * @param <R> the return type
   * @param key the key
   * @param fun the function to apply
   * @return the result of applying the function, wrapped in an Optional
   */
  public <R> Optional<R> withTime(K key, Function<Duration, R> fun) {
    var pos = timePos(key, 0);
    if (pos >= 0) {
      return Optional.of(fun.apply(timings.get(pos).getValue()));
    }
    return Optional.empty();
  }

  /**
   * Set the time of a key.
   *
   * @param time the duration from the start time
   * @param key the key
   */
  public void setTime(Duration time, K key) {
    if (key == null) {
      throw new IllegalArgumentException("Key cannot be null");
    }
    if (timings == null) {
      timings = new ArrayList<Map.Entry<K, Duration>>();
    }
    var pos = timePos(key, 0);
    if (time == null) {
      timings.remove(pos);
    } else {
      var entry = new AbstractMap.SimpleEntry<K, Duration>(key, time);
      if (pos >= 0) {
        timings.get(pos).setValue(time);
      } else {
        timings.add(entry);
      }
      timings.sort((entry1, entry2) -> entry1.getValue().compareTo(entry2.getValue()));
    }
  }

  /**
   * Set the time of a key.
   *
   * @param time the time
   * @param key the key
   */
  public void setTime(T time, K key) {
    checkStartTime();
    setTime(Duration.between(getStartTime(), time), key);
  }

  private void checkStartTime() {
    if (getStartTime() == null) {
      throw new IllegalStateException("startTime must be set");
    }
  }

  /**
   * Gets the duration of a key.
   *
   * @param key the key
   * @return the duration, wrapped in an Optional
   */
  public Optional<Duration> getDuration(K key) {
    return withTime(key, Function.identity());
  }

  public final Function<Duration, Temporal> timeOf = time -> getStartTime().plus(time);

  /**
   * Gets the time of a key.
   *
   * @param key the key
   * @return the time, wrapped in an Optional
   */
  public Optional<Temporal> getTime(K key) {
    if (getStartTime() != null) {
      return withTime(key, timeOf);
    }
    return Optional.empty();
  }

  /**
   * An iterator over the durations,
   * after applying a function.
   *
   * @param <R> the return type
   * @param fun the function to apply
   * @return the iterator
   */
  public <R> Iterator<R> durations(Function<Duration, R> fun) {
    return new Iterator<R>() {

      private int pos = 0;

      @Override
      public boolean hasNext() {
        return timings != null && pos < timings.size();
      }

      @Override
      public R next() {
        return fun.apply(timings.get(pos++).getValue());
      }
    };
  }
  
  /**
   * An iterator over the durations.
   *
   * @return the iterator
   */
  public Iterator<Duration> durations() {
    return durations(Function.identity());
  }
  
  /**
   * An iterator over the times.
   *
   * @return the iterator
   */
  public Iterator<Temporal> times() {
    return durations(timeOf);
  }

  /**
   * An iterator over the times for specific keys,
   * after applying a function.
   *
   * @param <R> the return type
   * @param keys the keys
   * @param fun the function to apply
   * @return the iterator
   */
  public <R> Iterator<Optional<R>> times(Iterator<K> keys, Function<Duration, R> fun) {
    return new Iterator<Optional<R>>() {

      @Override
      public boolean hasNext() {
        return keys.hasNext();
      }

      @Override
      public Optional<R> next() {
        var duration = getDuration(keys.next());
        return  (duration.isPresent() ? Optional.of(fun.apply(duration.get())) : Optional.empty());
      }
    };
  }
}
