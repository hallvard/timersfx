package no.hal.timers.mobile;

import javafx.application.Application;
import javafx.stage.Stage;
import no.hal.timers.fx.TimersFxApp;

/**
 * Starts the user interface as a native app, on Android, built with Gluon (GraalVM native image), and on the desktop,
 * with {@code mvn -Pmobile -pl mobile gluonfx:run}. The window is the whole screen.
 */
public class MobileApp extends Application {

  @Override
  public void start(Stage stage) throws Exception {
    TimersFxApp.show(stage, true);
  }

  public static void main(String[] args) {
    launch(args);
  }
}
