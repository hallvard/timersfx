package no.hal.timers.core;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TimingsTest {

	private Timings<String, LocalTime> timings;

	@BeforeEach
	public void setUp() {
		timings = new Timings<String, LocalTime>();
	}

	@Test
	public void isInitiallyEmpty() {
		Assertions.assertTrue(timings.getCount() == 0);
	}

	@Test
	public void testSetTime() {
		timings.setStartTime(LocalTime.of(12, 0));
		timings.setTime(LocalTime.of(12, 10), "12:10");
		Assertions.assertEquals(Duration.ofMinutes(10), timings.getDuration("12:10").get());
		timings.setTime(Duration.ofMinutes(20), "12:20");
		Assertions.assertEquals(LocalTime.of(12, 20), timings.getTime("12:20").get());
	}

	private <T> void checkTimes(final Iterator<T> times1, @SuppressWarnings("unchecked") final T... times2) {
		for (int i = 0; i < times2.length; i++) {
			Assertions.assertTrue(times1.hasNext());
			Assertions.assertEquals(times2[i], times1.next());
		}
		Assertions.assertFalse(times1.hasNext());
	}

	@Test
	public void testTimes0() {
		timings.setStartTime(LocalTime.now());
		checkTimes(timings.times());
		timings.setTime(LocalTime.of(12, 10), "12:10");
		checkTimes(timings.times(), LocalTime.of(12, 10));
		timings.setTime(LocalTime.of(12, 20), "12:20");
		checkTimes(timings.times(), LocalTime.of(12, 10), LocalTime.of(12, 20));
		timings.setTime(LocalTime.of(12, 0), "12:20");
		checkTimes(timings.times(), LocalTime.of(12, 0), LocalTime.of(12, 10));
		timings.setTime(LocalTime.of(11, 50), "11:50");
		checkTimes(timings.times(), LocalTime.of(11, 50), LocalTime.of(12, 0), LocalTime.of(12, 10));
	}
}
