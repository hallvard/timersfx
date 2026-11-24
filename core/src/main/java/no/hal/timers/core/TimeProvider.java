package no.hal.timers.core;

import java.time.LocalTime;
import java.util.function.Supplier;

/**
 * A clock, i.e. a provider of LocalTime instances.
 * Mainly for testing purposes.
 */
public interface TimeProvider extends Supplier<LocalTime> {

  TimeProvider SYSTEM_TIME_PROVIDER = () -> LocalTime.now();
}
