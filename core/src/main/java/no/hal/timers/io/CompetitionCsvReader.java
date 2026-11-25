package no.hal.timers.io;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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

    // the column with the start time marks the format written by the writer, where the times are since the start
    var startCol = -1;
    for (var colNum = 1; colNum < headers.length; colNum++) {
      if (CompetitionWriter.START_KEY.equals(headers[colNum])) {
        startCol = colNum;
      }
    }
    var timesAreSinceStart = startCol >= 0;

    if (timesAreSinceStart) {
      // all the other columns are timing keys, also those without any times
      for (var colNum = 1; colNum < headers.length; colNum++) {
        if (colNum != startCol) {
          timingKeys.add(headers[colNum]);
        }
      }
    } else {
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
    }
    var comp = new Competition();
    comp.setTimingKeys(timingKeys.toArray(new String[timingKeys.size()]));
    // the last timing key is the finish
    var finishCol = -1;
    for (var colNum = 1; colNum < headers.length; colNum++) {
      if (colNum != startCol && timingKeys.contains(headers[colNum])) {
        finishCol = colNum;
      }
    }

    for (int rowNum = 1; rowNum < allRows.size(); rowNum++) {
      var row = allRows.get(rowNum);
      var name = row[0] != null ? row[0].trim() : "";
      var participation = comp.addParticipant(new Participant(name));
      if (startCol >= 0 && startCol < row.length && row[startCol] != null) {
        var startTime = parseTime(row[startCol].trim());
        if (startTime != null) {
          participation.start(startTime);
        }
      }
      var total = Duration.ZERO;
      for (var colNum = 1; colNum < headers.length; colNum++) {
        var header = headers[colNum];
        if (colNum != startCol && timingKeys.contains(header)) {
          if (colNum < row.length && row[colNum] != null) {
            var duration = parseDuration(row[colNum].trim());
            if (duration != null) {
              total = total.plus(duration);
              var isFinish = colNum == finishCol;
              var time = (timesAreSinceStart || columnsAreTotals || (isFinish && lastColumnIsTotal)
                  ? duration
                  : total);
              var timingKey = (isFinish ? Participation.Status.FINISH.name() : header);
              participation.time(time, timingKey);
            }
          }
        }
      }
    }
    return comp;
  }

  /** Parses a clock time, HH:mm:ss or HH:mm, or returns null if it isn't one. */
  LocalTime parseTime(String value) {
    try {
      return LocalTime.parse(value);
    } catch (DateTimeParseException e) {
      return null;
    }
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
