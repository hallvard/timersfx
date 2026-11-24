package no.hal.timers.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Iterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for Timings.
 */
public class TimingsTest {

  private Timings<String, LocalTime> timings;

  /**
   * Set up the Timings instance.
   */
  @BeforeEach
  public void setUp() {
    timings = new Timings<String, LocalTime>();
  }

  @Test
  public void isInitiallyEmpty() {
    assertTrue(timings.getCount() == 0);
  }

  @Test
  public void testSetTime() {
    timings.setStartTime(LocalTime.of(12, 0));
    timings.setTime(LocalTime.of(12, 10), "12:10");
    assertEquals(Duration.ofMinutes(10), timings.getDuration("12:10").get());
    timings.setTime(Duration.ofMinutes(20), "12:20");
    assertEquals(LocalTime.of(12, 20), timings.getTime("12:20").get());
  }

  private <T> void checkTimes(Iterator<T> times1, @SuppressWarnings("unchecked") T... times2) {
    for (int i = 0; i < times2.length; i++) {
      assertTrue(times1.hasNext());
      assertEquals(times2[i], times1.next());
    }
    assertFalse(times1.hasNext());
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
    checkTimes(timings.times(), LocalTime.of(11, 50), LocalTime.of(12, 0),
        LocalTime.of(12, 10));
  }
}
