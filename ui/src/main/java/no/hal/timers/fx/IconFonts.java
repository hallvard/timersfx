package no.hal.timers.fx;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.text.Font;

/**
 * Registers the icon font (Ikonli, Material Design Icons 2) by name, before the first icon is made. Ikonli loads its font itself from a
 * url, which gives no font in a native app on Android (the icons are boxes): JavaFX copies the font to a temporary file first, which
 * fails there. A stream, or failing that a file in the temporary directory (the private directory of the app), does give a font.
 */
final class IconFonts {

  private static final String FONT_FILE = "/META-INF/resources/materialdesignicons2/5.8.55/fonts/materialdesignicons-webfont.ttf";
  private static boolean loaded;

  private IconFonts() {
  }

  static synchronized void load() {
    if (loaded) {
      return;
    }
    loaded = true;
    try {
      byte[] bytes;
      try (var stream = IconFonts.class.getResourceAsStream(FONT_FILE)) {
        bytes = stream.readAllBytes();
      }
      var font = Font.loadFont(new ByteArrayInputStream(bytes), 16);
      if (font == null) {
        var file = Path.of(System.getProperty("java.io.tmpdir"), "timersfx-mdi2.ttf");
        Files.write(file, bytes);
        font = Font.loadFont(file.toUri().toString(), 16);
      }
      if (font == null) {
        System.err.println("The icon font could not be loaded, so its icons are boxes");
      }
    } catch (Exception e) {
      System.err.println("The icon font could not be loaded, so its icons are boxes: " + e);
    }
  }
}
