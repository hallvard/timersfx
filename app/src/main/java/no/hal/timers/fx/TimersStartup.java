package no.hal.timers.fx;

import io.quarkiverse.fx.FxPostStartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Shows the window for timing when the application has started, e.g. with {@code mvn quarkus:dev}. */
@ApplicationScoped
public class TimersStartup {

  void start(@Observes FxPostStartupEvent startupEvent) {
    try {
      TimersFxApp.show(startupEvent.getPrimaryStage(), false);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
