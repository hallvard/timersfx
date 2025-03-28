package no.hal.timers.io;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import no.hal.timers.core.Competition;
import no.hal.timers.core.Participant;
import no.hal.timers.core.Participation;

/**
 * Reads a competition from a CSV source.
 */
public class CompetitionCsvReader {

  private final boolean columnsAreTotals = false;
  private final boolean lastColumnIsTotal = true;
  private final boolean emptyColumnsAreTime = true;

  /**
   * Reads a competition from a CSV file.
   *
   * @param input the input stream
   * @return the resulting competition
   * @throws Exception if error occurs
   */
  public Competition readCompetition(InputStream input) {
    var settings = new CsvParserSettings();
    settings.getFormat().setLineSeparator("\n");
    settings.getFormat().setDelimiter(',');
    var parser = new CsvParser(settings);

    // parses all rows in one go
    var allRows = parser.parseAll(new InputStreamReader(input, StandardCharsets.UTF_8));
    var timingKeys = new ArrayList<String>();
    var headers = allRows.get(0);

    // guess which headers are timing keys, by looking at first line with value
    for (var colNum = 1; colNum < headers.length; colNum++) {
      var header = headers[colNum];
      var allEmpty = true;
      for (var rowNum = 1; rowNum < allRows.size(); rowNum++) {
        var row = allRows.get(rowNum);
        if (colNum < row.length) {
          var value = row[colNum].trim();
          if (value.length() > 0) {
            allEmpty = false;
          }
          if (isTiming(value)) {
            timingKeys.add(header);
            break;
          }
        }
      }
      if (emptyColumnsAreTime && allEmpty) {
        timingKeys.add(header);
      }
    }
    var comp = new Competition();
    comp.setTimingKeys(timingKeys.toArray(new String[timingKeys.size()]));

    for (int rowNum = 1; rowNum < allRows.size(); rowNum++) {
      var row = allRows.get(rowNum);
      var name = row[0].trim();
      var participant = new Participant(name);
      var participation = comp.addParticipant(participant);
      var total = Duration.ZERO;
      for (var colNum = 1; colNum < headers.length; colNum++) {
        var header = headers[colNum];
        if (timingKeys.contains(header)) {
          if (colNum < row.length) {
            var nthLast = headers.length - colNum - 1;
            var duration = parseDuration(row[colNum].trim());
            if (duration != null) {
              total = total.plus(duration);
              var time = (columnsAreTotals || (nthLast == 0 && lastColumnIsTotal)
                  ? duration
                  : total);
              var timingKey = (nthLast == 0 ? Participation.Status.FINISH.name() : header);
              participation.time(time, timingKey);
            }
          }
        }
      }
    }
    return comp;
  }

  private boolean isTiming(String value) {
    if (parseDuration(value) != null) {
      return true;
    }
    return false;
  }

  Duration parseDuration(String value) {
    var split = value.split(":");
    var times = new int[split.length];
    for (var i = 0; i < split.length; i++) {
      try {
        times[i] = Integer.valueOf(split[i]);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    int[] durations = { 60, 60, 1 };
    var seconds = 0;
    var duration = 1;
    for (var i = 0; i < times.length; i++) {
      duration *= durations[durations.length - 1 - i];
      seconds += times[times.length - 1 - i] * duration;
    }
    return Duration.of(seconds, ChronoUnit.SECONDS);
  }
}
