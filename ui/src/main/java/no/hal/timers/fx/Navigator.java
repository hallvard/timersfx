package no.hal.timers.fx;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import no.hal.timers.core.Competition;
import no.hal.timers.core.Participant;
import no.hal.timers.io.EventStore;

/**
 * Switches between the list of events and the table of one event, by changing the root of the scene, since a second window doesn't
 * suit the native app. It also keeps an event saved while it is shown.
 */
final class Navigator {

  private final Scene scene;
  private final EventStore store;
  private final Settings settings;
  private ResourceBundle bundle;

  Navigator(Scene scene, EventStore store, Settings settings) {
    this.scene = scene;
    this.store = store;
    this.settings = settings;
    applyLanguage();
  }

  /**
   * Uses the language of the settings for the texts. The default locale is set too, so the bundle of the language is found
   * also when it is not the language of the device.
   */
  void applyLanguage() {
    Locale.setDefault(settings.getLocale());
    bundle = ResourceBundle.getBundle("no.hal.timers.fx.TimersFxApp");
  }

  private Object currentController;

  /** The controller of the screen that is shown. */
  Object getCurrentController() {
    return currentController;
  }

  private FXMLLoader load(String fxml) throws IOException {
    var loader = new FXMLLoader(Navigator.class.getResource(fxml), bundle);
    loader.load();
    currentController = loader.getController();
    return loader;
  }

  void showList() {
    try {
      var loader = load("EventList.fxml");
      ((EventListController) loader.getController()).setNavigator(this, store);
      scene.setRoot(loader.getRoot());
    } catch (Throwable e) {
      scene.setRoot(createErrorRoot(e));
    }
  }

  void showSettings() {
    try {
      var loader = load("Settings.fxml");
      ((SettingsController) loader.getController()).setNavigator(this, settings);
      scene.setRoot(loader.getRoot());
    } catch (Throwable e) {
      scene.setRoot(createErrorRoot(e));
    }
  }

  /**
   * A screen with the text of an error, instead of a black screen, which is what a native app shows when it fails to start
   * and has nowhere to tell why.
   */
  static Parent createErrorRoot(Throwable error) {
    var text = new StringWriter();
    error.printStackTrace(new PrintWriter(text));
    System.err.println(text);
    var area = new TextArea(text.toString());
    area.setEditable(false);
    area.setWrapText(true);
    return new BorderPane(area);
  }

  void openEvent(String baseName) {
    Competition competition;
    try {
      competition = store.load(baseName);
    } catch (IOException e) {
      System.err.println("The event " + baseName + " could not be opened: " + e);
      return;
    }
    try {
      var loader = load("TimersFxApp.fxml");
      ((TimersFxAppController) loader.getController()).setEvent(competition, () -> save(baseName, competition), this::showList);
      scene.setRoot(loader.getRoot());
    } catch (Throwable e) {
      scene.setRoot(createErrorRoot(e));
    }
  }

  private void save(String baseName, Competition competition) {
    try {
      store.save(baseName, competition);
    } catch (IOException e) {
      System.err.println("The event " + baseName + " could not be saved: " + e);
    }
  }

  /** The table of a new event: some participants and timing keys to change, where the last timing key finishes. */
  Competition createDefaultCompetition() {
    var competition = new Competition();
    var timingKeys = new String[4];
    for (var i = 0; i < timingKeys.length - 1; i++) {
      timingKeys[i] = String.format(bundle.getString("timingKeyFormat"), i + 1);
    }
    timingKeys[timingKeys.length - 1] = bundle.getString("finishLabel");
    competition.setTimingKeys(timingKeys);
    for (var i = 0; i < 5; i++) {
      competition.addParticipant(new Participant(String.format(bundle.getString("participantNameFormat"), i + 1)));
    }
    return competition;
  }
}
