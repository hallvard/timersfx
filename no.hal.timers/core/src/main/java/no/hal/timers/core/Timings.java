package no.hal.timers.core;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Timings<K, T extends Temporal & Comparable<T>> {

	private T startTime = null;
	private List<Map.Entry<K, Duration>> timings;

	public int getCount() {
		return (timings != null ? timings.size() : 0);
	}

	public Timings() {
	}

	public Timings(final T time) {
		setStartTime(time);
	}

	public T getStartTime() {
		return startTime;
	}

	public void setStartTime(final T startTime) {
		this.startTime = startTime;
	}

	private int timePos(final K key, final int start) {
		if (timings != null) {
			for (int i = start; i < timings.size(); i++) {
				if (timings.get(i).getKey().equals(key)) {
					return i;
				}
			}
		}
		return -1;
	}

	public boolean hasTime(final K key) {
		return timePos(key, 0) >= 0;
	}

	public <R> Optional<R> withTime(final K key, final Function<Duration, R> fun) {
		final int pos = timePos(key, 0);
		if (pos >= 0) {
			return Optional.of(fun.apply(timings.get(pos).getValue()));
		}
		return Optional.empty();
	}

	public void setTime(final Duration time, final K key) {
		if (key == null) {
			throw new IllegalArgumentException("Key cannot be null");
		}
		if (timings == null) {
			timings = new ArrayList<Map.Entry<K, Duration>>();
		}
		final int pos = timePos(key, 0);
		if (time == null) {
			timings.remove(pos);
		} else {
			final SimpleEntry<K, Duration> entry = new AbstractMap.SimpleEntry<K, Duration>(key, time);
			if (pos >= 0) {
				timings.get(pos).setValue(time);
			} else {
				timings.add(entry);
			}
			timings.sort((entry1, entry2) -> entry1.getValue().compareTo(entry2.getValue()));
		}
	}
	public void setTime(final T time, final K key) {
		checkStartTime();
		setTime(Duration.between(getStartTime(), time), key);
	}

	private void checkStartTime() {
		if (getStartTime() == null) {
			throw new IllegalStateException("startTime must be set");
		}
	}

	public Optional<Duration> getDuration(final K key) {
		return withTime(key, Function.identity());
	}

	public Function<Duration, Temporal> timeOf = time -> getStartTime().plus(time);

	public Optional<Temporal> getTime(final K key) {
		if (getStartTime() != null) {
			return withTime(key, timeOf);
		}
		return Optional.empty();
	}

	public <R> Iterator<R> durations(final Function<Duration, R> fun) {
		return new Iterator<R>() {

			private int i = 0;

			@Override
			public boolean hasNext() {
				return timings != null && i < timings.size();
			}

			@Override
			public R next() {
				return fun.apply(timings.get(i++).getValue());
			}
		};
	}
	public Iterator<Duration> durations() {
		return durations(Function.identity());
	}
	public Iterator<Temporal> times() {
		return durations(timeOf);
	}

	public <R> Iterator<Optional<R>> times(final Iterator<K> keys, final Function<Duration, R> fun) {
		return new Iterator<Optional<R>>() {

			@Override
			public boolean hasNext() {
				return keys.hasNext();
			}

			@Override
			public Optional<R> next() {
				final Optional<Duration> duration = getDuration(keys.next());
				return  (duration.isPresent() ? Optional.of(fun.apply(duration.get())) : Optional.empty());
			}
		};
	}
}
