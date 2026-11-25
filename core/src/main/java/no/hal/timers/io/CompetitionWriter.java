package no.hal.timers.io;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import no.hal.timers.core.Competition;

/**
 * Writes a competition to a target, e.g. HTML table or CSV.
 */
public class CompetitionWriter {

  public interface Target extends AutoCloseable {
    void acceptHeaders(Map<String, String> headers);
    void acceptRow(Map<String, String> row);
  }

  static final String PARTICIPANT_KEY = "Participant";

  /**
   * The header of the column with the start time (as a clock time, HH:mm:ss). It marks the format written here, where the other
   * times are the time since the start, also for the laps. Files without it, as the sample, have the time of each lap
   * (since the previous one) and the total in the last column, see {@link CompetitionCsvReader}.
   */
  public static final String START_KEY = "Start";

  /**
   * Writes a competition to a target, e.g. HTML table or CSV.
   *
   * @param comp the competition
   * @param output the output stream
   * @throws Exception if error occurs
   */
  public void writeCompetition(Competition comp, Target target) {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put(PARTICIPANT_KEY, PARTICIPANT_KEY);
    headers.put(START_KEY, START_KEY);
    var timingKeys = comp.getTimingKeys();
    for (var timingKey : timingKeys) {
      headers.put(timingKey, timingKey);
    }
    target.acceptHeaders(headers);
    for (var participation : comp) {
      Map<String, String> row = new LinkedHashMap<>();
      row.put(PARTICIPANT_KEY, participation.getParticipant().getName());
      row.put(START_KEY, participation.getStartTime()
          .map(time -> time.truncatedTo(ChronoUnit.SECONDS).toString())
          .map(text -> text.length() == 5 ? text + ":00" : text)
          .orElse(""));
      // the last timing key is the finish, which is recorded under FINISH whatever it is named
      for (var i = 0; i < timingKeys.size(); i++) {
        var duration = participation.getDuration(comp.getTimeKey(i));
        row.put(timingKeys.get(i), duration.map(this::formatDuration).orElse(""));
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
    if (buffer.length() == 0) {
      // a time that is zero seconds is still a time
      buffer.append("00");
    }
    return buffer.toString();
  }
}
