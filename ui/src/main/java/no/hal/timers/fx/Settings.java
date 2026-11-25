package no.hal.timers.fx;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * The settings of the app, stored in a properties file. Only the language so far.
 */
final class Settings {

  static final String LANGUAGE_KEY = "language";

  private final Path file;
  private final Properties properties = new Properties();

  /** Reads the settings from the file, if it exists. A file that cannot be read gives the default settings. */
  Settings(Path file) {
    this.file = file;
    if (Files.exists(file)) {
      try (InputStream in = Files.newInputStream(file)) {
        properties.load(in);
      } catch (IOException e) {
        System.err.println("The settings could not be read: " + e);
      }
    }
  }

  /** The language tag that is chosen, or an empty string when it has not been chosen. */
  String getLanguage() {
    return properties.getProperty(LANGUAGE_KEY, "");
  }

  void setLanguage(String language) {
    properties.setProperty(LANGUAGE_KEY, language);
  }

  /**
   * The locale to use: the chosen language, or else the locale of the device. A device where the locale is only {@code en},
   * without a country, is the native Android app, which doesn't get the language of the phone, so then it is Norwegian bokmål.
   */
  Locale getLocale() {
    var language = getLanguage();
    if (! language.isEmpty()) {
      return Locale.forLanguageTag(language);
    }
    var locale = Locale.getDefault();
    if (locale.getLanguage().equals("en") && locale.getCountry().isEmpty()) {
      return Locale.forLanguageTag("nb");
    }
    return locale;
  }

  void save() {
    try {
      var parent = file.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      try (OutputStream out = Files.newOutputStream(file)) {
        properties.store(out, "TimersFX settings");
      }
    } catch (IOException e) {
      System.err.println("The settings could not be saved: " + e);
    }
  }
}
