package no.hal.timers.fx;

import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

/**
 * Controller for the settings screen, where the language is chosen.
 */
public class SettingsController {

  /** A language that can be chosen, shown by its own name, which is the same whatever the language of the app is. */
  record Language(String tag, String name) {
    @Override
    public String toString() {
      return name;
    }
  }

  static final List<Language> LANGUAGES = List.of(
      new Language("en", "English"),
      new Language("nb", "Norsk bokmål"),
      new Language("nn", "Norsk nynorsk"));

  @FXML
  ResourceBundle resources;

  @FXML
  ComboBox<Language> languageBox;

  private Navigator navigator;
  private Settings settings;

  void setNavigator(Navigator navigator, Settings settings) {
    this.navigator = navigator;
    this.settings = settings;
    languageBox.getItems().setAll(LANGUAGES);
    var current = settings.getLocale().getLanguage();
    // the language no is Norwegian, which is shown as bokmål
    languageBox.getSelectionModel().select(LANGUAGES.stream()
        .filter(language -> language.tag().equals(current.equals("no") ? "nb" : current))
        .findFirst().orElse(LANGUAGES.get(0)));
    languageBox.valueProperty().addListener((prop, oldLanguage, language) -> {
      if (language != null && oldLanguage != null) {
        settings.setLanguage(language.tag());
        settings.save();
        navigator.applyLanguage();
        // shows this screen again, in the new language, when the choice is done
        Platform.runLater(navigator::showSettings);
      }
    });
  }

  @FXML
  void handleBack() {
    navigator.showList();
  }
}
