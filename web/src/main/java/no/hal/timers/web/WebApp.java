package no.hal.timers.web;

import javafx.application.Application;
import javafx.stage.Stage;
import no.hal.timers.fx.TimersFxApp;

/** Starts the user interface for each user who opens the page, when run by JPro, which shows the window in the browser. */
public class WebApp extends Application {

  @Override
  public void start(Stage stage) throws Exception {
    TimersFxApp.show(stage, false);
  }

  /** For running it as an ordinary desktop program too, with {@code mvn -Pweb -pl web javafx:run} or from an IDE. */
  public static void main(String[] args) {
    launch(args);
  }
}
