package no.hal.timers.core;

import java.time.LocalTime;
import java.util.function.Supplier;

public interface TimeProvider extends Supplier<LocalTime> {
}
