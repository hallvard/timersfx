package no.hal.timers.fx;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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

  /**
   * Shows the user interface in the stage. Used by the desktop (app), web and mobile modules.
   *
   * @param stage the stage to show the user interface in
   * @param fullScreen whether the window is the whole screen, as on a phone
   */
  public static void show(Stage stage, boolean fullScreen) throws IOException {
    IconFonts.load();
    Parent parent = FXMLLoader.load(TimersFxApp.class.getResource("TimersFxApp.fxml"),
        ResourceBundle.getBundle("no.hal.timers.fx.TimersFxApp"));
    if (!fullScreen) {
      stage.setX(20);
      stage.setY(20);
    }
    stage.setScene(new Scene(parent));
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
