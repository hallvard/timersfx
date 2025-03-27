package no.hal.timers.fx;

import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The main class for the TimersFX application.
 */
public class TimersFxApp extends Application {

  @Override
  public void start(Stage primaryStage) throws Exception {
    Parent parent = FXMLLoader.load(TimersFxApp.class.getResource("TimersFxApp.fxml"),
        ResourceBundle.getBundle("no.hal.timers.fx.TimersFxApp"));
    primaryStage.setX(20);
    primaryStage.setY(20);
    primaryStage.setScene(new Scene(parent));
    primaryStage.show();
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
