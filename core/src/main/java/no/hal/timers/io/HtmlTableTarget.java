package no.hal.timers.io;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import no.hal.timers.core.Competition;

/**
 * A CSV target, for writing a Competition to a CSV file.
 */
public class HtmlTableTarget implements CompetitionWriter.Target {

  private PrintWriter writer;

  public HtmlTableTarget(OutputStream output) {
    this.writer = new PrintWriter(output);
  }

  @Override
  public void acceptHeaders(Map<String, String> headers) {
    this.writer.println("<table>");
    this.writer.print("  <tr>");
    for (var header : headers.values()) {
      this.writer.printf("<th>%s</th>", header);
    }
    this.writer.println("</tr>");
  }

  @Override
  public void acceptRow(Map<String, String> row) {
    this.writer.print("  <tr>");
    for (var value : row.values()) {
      this.writer.printf("<td>%s</td>", value);
    }
    this.writer.println("</tr>");
  }

  @Override
  public void close() throws Exception {
    this.writer.println("</table>");
    this.writer.close();
  }

  public static String toString(Competition comp) {
    var compWriter = new CompetitionWriter();
    ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
    try (var output = byteArrayOutput;
         var compTarget = new HtmlTableTarget(output)) {
      compWriter.writeCompetition(comp, compTarget);
    } catch (Exception e) {
      return e.getMessage();
    }
    return new String(byteArrayOutput.toByteArray());
  }
}
