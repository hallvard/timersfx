package no.hal.timers.fxui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

	@Override
	public void start(final Stage primaryStage) throws Exception {
        final Parent parent = FXMLLoader.load(this.getClass().getResource("App.fxml"));
        primaryStage.setX(20);
        primaryStage.setY(20);
		primaryStage.setScene(new Scene(parent));
		primaryStage.show();
	}

	public static void main(final String[] args) {
		launch(args);
	}
}
