package no.hal.timers.io;

import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;
import no.hal.timers.core.Competition;

/**
 * A CSV target, for writing a Competition to a CSV file.
 */
public class CsvTarget implements CompetitionWriter.Target {

  private CsvWriter writer;

  public CsvTarget(OutputStream output) {
    var settings = new CsvWriterSettings();
    settings.getFormat().setLineSeparator("\n");
    settings.getFormat().setDelimiter(',');
    this.writer = new CsvWriter(output, settings);
  }

  @Override
  public void acceptHeaders(Map<String, String> headers) {
    this.writer.writeHeaders(headers.values());
  }

  @Override
  public void acceptRow(Map<String, String> row) {
    this.writer.writeRow(row.values());
  }

  @Override
  public void close() throws Exception {
    this.writer.close();
  }

  public static String toString(Competition comp) {
    var compWriter = new CompetitionWriter();
    ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
    try (var output = byteArrayOutput;
         var compTarget = new CsvTarget(output)) {
      compWriter.writeCompetition(comp, compTarget);
    } catch (Exception e) {
      return e.getMessage();
    }
    return new String(byteArrayOutput.toByteArray());
  }
}
