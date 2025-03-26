package no.hal.timers.csv;

import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import no.hal.timers.core.Competition;

/**
 * Reads a competition from a CSV file.
 */
public class CompetitionCsvWriter {

  /**
   * Writes a competition as CSV.
   *
   * @param comp the competition
   * @param output the output stream
   * @throws Exception if error occurs
   */
  public void writeCompetition(Competition comp, OutputStream output) {
    var settings = new CsvWriterSettings();
    settings.getFormat().setLineSeparator("\n");
    settings.getFormat().setDelimiter(',');
    var writer = new CsvWriter(output, settings);

    List<String> headers = new ArrayList<>();
    headers.add("Participant");
    headers.addAll(comp.getTimingKeys());

    writer.writeHeaders(headers);
    for (var participation : comp) {
      var row = new ArrayList<String>();
      row.add(participation.getParticipant().getName());
      for (var timingKey : comp.getTimingKeys()) {
        var duration = participation.getDuration(timingKey);
        row.add(duration.map(this::formatDuration).orElse(""));
      }
      writer.writeRow(row);
    }
    writer.close();
  }

  String formatDuration(Duration duration) {
    var buffer = new StringBuilder();
    int[] durations = { 24, 60, 60 };
    long value = duration.get(ChronoUnit.SECONDS);
    for (int i = 0; i < durations.length && value > 0; i++) {
      long time = value % durations[durations.length - i - 1];
      if (buffer.length() > 0) {
        buffer.insert(0, ":");
      }
      buffer.insert(0, time);
      if (time < 10) {
        buffer.insert(0, "0");
      }
      value = value / durations[durations.length - i - 1];
    }
    return buffer.toString();
  }

  public String toString(Competition comp) {
    try (var output = new ByteArrayOutputStream()) {
      writeCompetition(comp, output);
      return new String(output.toByteArray());
    } catch (IOException e) {
      // ignore
      return e.getMessage();
    }
  }
}
