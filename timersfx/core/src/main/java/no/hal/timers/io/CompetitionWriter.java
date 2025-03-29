package no.hal.timers.io;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import no.hal.timers.core.Competition;

/**
 * Writes a competition to a target, e.g. HTML table or CSV.
 */
public class CompetitionWriter {

  public interface Target extends AutoCloseable {
    void acceptHeaders(Map<String, String> headers);
    void acceptRow(Map<String, String> row);
  }

  private static String participantKey = "Participant";

  /**
   * Writes a competition to a target, e.g. HTML table or CSV.
   *
   * @param comp the competition
   * @param output the output stream
   * @throws Exception if error occurs
   */
  public void writeCompetition(Competition comp, Target target) {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(participantKey, participantKey);
    headers.putAll(comp.getTimingKeys().stream().collect(Collectors.toMap(key -> key, key -> key)));
    target.acceptHeaders(headers);
    for (var participation : comp) {
      Map<String, String> row = new LinkedHashMap<>();
      row.put(participantKey, participation.getParticipant().getName());
      for (var timingKey : comp.getTimingKeys()) {
        var duration = participation.getDuration(timingKey);
        row.put(timingKey, duration.map(this::formatDuration).orElse(""));
      }
      target.acceptRow(row);
    }
  }

  public String formatDuration(Duration duration) {
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
}
