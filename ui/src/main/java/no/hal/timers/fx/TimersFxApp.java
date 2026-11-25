package no.hal.timers.fx;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import no.hal.timers.io.EventStore;

/**
 * The main class for the TimersFX application.
 */
public class TimersFxApp extends Application {

  private static Consumer<String> resultsSharer;

  /** Sets how the results (CSV text) are got out of the app, instead of copying them to the clipboard. */
  public static void setResultsSharer(Consumer<String> sharer) {
    resultsSharer = sharer;
  }

  static Consumer<String> getResultsSharer() {
    return resultsSharer;
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    show(primaryStage, false);
  }

  /** The directory of the events: in the home directory of the user, which on Android is the private directory of the app. */
  static Path defaultStoreDirectory() {
    return Path.of(System.getProperty("timersfx.dir",
        Path.of(System.getProperty("user.home"), ".timersfx", "events").toString()));
  }

  /**
   * Shows the user interface in the stage, which starts with the list of events. Used by the desktop (app), web and mobile modules.
   *
   * @param stage the stage to show the user interface in
   * @param fullScreen whether the window is the whole screen, as on a phone
   */
  public static void show(Stage stage, boolean fullScreen) throws IOException {
    var scene = fullScreen ? new Scene(new Group()) : new Scene(new Group(), 520, 720);
    try {
      IconFonts.load();
      var bundle = ResourceBundle.getBundle("no.hal.timers.fx.TimersFxApp");
      new Navigator(scene, new EventStore(defaultStoreDirectory()), bundle).showList();
    } catch (Throwable e) {
      // shown, and not a black screen
      scene.setRoot(Navigator.createErrorRoot(e));
    }
    if (! fullScreen) {
      stage.setX(20);
      stage.setY(20);
    }
    stage.setScene(scene);
    stage.show();
  }

  /**
   * Launches the TimersFX application.
   *
   * @param args the command-line arguments
   */
  public static void main(String[] args) {
    launch(args);
  }
}
