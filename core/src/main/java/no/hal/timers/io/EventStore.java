package no.hal.timers.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import no.hal.timers.core.Competition;
import no.hal.timers.core.Participant;

/**
 * Keeps the events (competitions) in a directory. An event is two files with the same base name, which is the date and the location,
 * e.g. {@code 2026-11-25-storsvingen}: a CSV file with the table (see {@link CompetitionWriter}), and a properties file with the rest,
 * that is the date and the location.
 */
public class EventStore {

  private static final String CSV_EXTENSION = ".csv";
  private static final String PROPERTIES_EXTENSION = ".properties";
  private static final String DATE_KEY = "date";
  private static final String LOCATION_KEY = "location";

  /** What a location can be: letters, digits, dash and underscore, so the name of an event is a valid file name. */
  private static final Pattern LOCATION_PATTERN = Pattern.compile("[\\p{L}\\p{N}_-]*");

  /**
   * What the list of events needs to show about an event.
   *
   * @param baseName the base name of the files, which identifies the event
   * @param date the date
   * @param location the location, which may be empty
   * @param participantCount the number of participants
   */
  public record EventInfo(String baseName, LocalDate date, String location, int participantCount) {
  }

  private final Path directory;

  public EventStore(Path directory) {
    this.directory = directory;
  }

  public static boolean isValidLocation(String location) {
    return location != null && LOCATION_PATTERN.matcher(location).matches();
  }

  /** The base name of the files for an event, the date followed by the location, if there is one. */
  public static String baseName(LocalDate date, String location) {
    return date + (location.isEmpty() ? "" : "-" + location);
  }

  private Path csvFile(String baseName) {
    return directory.resolve(baseName + CSV_EXTENSION);
  }

  private Path propertiesFile(String baseName) {
    return directory.resolve(baseName + PROPERTIES_EXTENSION);
  }

  private void checkBaseName(String baseName) {
    // the base name is used as a file name, so it must not be able to leave the directory
    if (baseName.isEmpty() || ! baseName.matches("[\\p{L}\\p{N}_-]+")) {
      throw new IllegalArgumentException("Not a valid name of an event: " + baseName);
    }
  }

  private void checkLocation(String location) {
    if (! isValidLocation(location)) {
      throw new IllegalArgumentException(
          "A location can only have letters, digits, - and _: " + location);
    }
  }

  /** Lists the events, the newest first. Files that can't be read are left out. */
  public List<EventInfo> list() throws IOException {
    var events = new ArrayList<EventInfo>();
    if (! Files.isDirectory(directory)) {
      return events;
    }
    try (var files = Files.list(directory)) {
      for (var file : (Iterable<Path>) files::iterator) {
        var fileName = file.getFileName().toString();
        if (fileName.endsWith(PROPERTIES_EXTENSION)) {
          var baseName = fileName.substring(0, fileName.length() - PROPERTIES_EXTENSION.length());
          try {
            var properties = readProperties(baseName);
            var count = Files.exists(csvFile(baseName)) ? load(baseName).getParticipationCount() : 0;
            events.add(new EventInfo(baseName, LocalDate.parse(properties.getProperty(DATE_KEY)),
                properties.getProperty(LOCATION_KEY, ""), count));
          } catch (RuntimeException | IOException e) {
            // not an event of ours
          }
        }
      }
    }
    events.sort(Comparator.comparing(EventInfo::date).thenComparing(EventInfo::baseName).reversed());
    return events;
  }

  /** Loads the table of an event. */
  public Competition load(String baseName) throws IOException {
    checkBaseName(baseName);
    try (InputStream input = Files.newInputStream(csvFile(baseName))) {
      return new CompetitionCsvReader().readCompetition(input);
    }
  }

  /**
   * Saves the table of an event, replacing the file in one step, so a crash can't leave half a file.
   */
  public void save(String baseName, Competition competition) throws IOException {
    checkBaseName(baseName);
    Files.createDirectories(directory);
    var csv = CsvTarget.toString(competition).getBytes(StandardCharsets.UTF_8);
    replaceFile(csvFile(baseName), csv);
  }

  /**
   * Makes a new event, with the table.
   *
   * @return the base name of the new event
   * @throws FileAlreadyExistsException if there already is an event with that date and location
   */
  public String create(LocalDate date, String location, Competition competition) throws IOException {
    checkLocation(location);
    var baseName = baseName(date, location);
    checkBaseName(baseName);
    Files.createDirectories(directory);
    if (Files.exists(propertiesFile(baseName)) || Files.exists(csvFile(baseName))) {
      throw new FileAlreadyExistsException(baseName);
    }
    save(baseName, competition);
    var properties = new Properties();
    properties.setProperty(DATE_KEY, date.toString());
    properties.setProperty(LOCATION_KEY, location);
    var buffer = new java.io.StringWriter();
    properties.store(buffer, null);
    // Properties.store writes a line with the time as a comment, which is left out, so a file only changes when its contents do
    var text = String.join("\n", buffer.toString().lines().filter(line -> ! line.startsWith("#")).toList()) + "\n";
    replaceFile(propertiesFile(baseName), text.getBytes(StandardCharsets.UTF_8));
    return baseName;
  }

  /**
   * Makes a new event with the same participants and timing keys as another, but without any times.
   *
   * @return the base name of the new event
   */
  public String duplicate(String baseName, LocalDate date, String location) throws IOException {
    var source = load(baseName);
    var copy = new Competition();
    copy.setTimingKeys(source.getTimingKeys().toArray(new String[0]));
    source.forEach(participation -> copy.addParticipant(new Participant(participation.getParticipant().getName())));
    return create(date, location, copy);
  }

  /**
   * Changes the location of an event, which renames the files.
   *
   * @return the new base name
   * @throws FileAlreadyExistsException if there already is an event with that date and location
   */
  public String rename(String baseName, String newLocation) throws IOException {
    checkBaseName(baseName);
    checkLocation(newLocation);
    var properties = readProperties(baseName);
    var date = LocalDate.parse(properties.getProperty(DATE_KEY));
    var newBaseName = baseName(date, newLocation);
    if (newBaseName.equals(baseName)) {
      return baseName;
    }
    if (Files.exists(propertiesFile(newBaseName)) || Files.exists(csvFile(newBaseName))) {
      throw new FileAlreadyExistsException(newBaseName);
    }
    properties.setProperty(LOCATION_KEY, newLocation);
    var buffer = new java.io.StringWriter();
    properties.store(buffer, null);
    var text = String.join("\n", buffer.toString().lines().filter(line -> ! line.startsWith("#")).toList()) + "\n";
    // the table first, then the properties, which is what makes it an event, and last the old files are removed
    Files.move(csvFile(baseName), csvFile(newBaseName));
    replaceFile(propertiesFile(newBaseName), text.getBytes(StandardCharsets.UTF_8));
    Files.deleteIfExists(propertiesFile(baseName));
    return newBaseName;
  }

  /** Deletes an event. */
  public void delete(String baseName) throws IOException {
    checkBaseName(baseName);
    // the properties first, so a half deleted event isn't listed
    Files.deleteIfExists(propertiesFile(baseName));
    Files.deleteIfExists(csvFile(baseName));
  }

  private Properties readProperties(String baseName) throws IOException {
    var properties = new Properties();
    try (Reader reader = Files.newBufferedReader(propertiesFile(baseName), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private void replaceFile(Path file, byte[] bytes) throws IOException {
    var temporary = file.resolveSibling(file.getFileName() + ".tmp");
    Files.write(temporary, bytes);
    try {
      Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (java.nio.file.AtomicMoveNotSupportedException e) {
      Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
