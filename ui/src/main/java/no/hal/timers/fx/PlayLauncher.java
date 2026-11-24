package no.hal.timers.fx;

import javafx.application.Application;

/** Main class for the executable jar (profile play), which isn't an {@link Application}, so JavaFX is found on the class path. */
public class PlayLauncher {

  public static void main(String[] args) {
    Application.launch(TimersFxApp.class, args);
  }
}
