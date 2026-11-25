package no.hal.timers.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import no.hal.timers.core.Competition;
import no.hal.timers.core.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CompetitionWriterTest {
  
    private CompetitionWriter compWriter;

  /**
   * Set up the CompetitionCsvWriter instance.
   */
  @BeforeEach
  public void setUp() {
    compWriter = new CompetitionWriter();
  }

  @Test
  public void testFormatDuration() {
    assertEquals("01:01:01", compWriter.formatDuration(Duration.ofSeconds(3661)));
    assertEquals("01:01", compWriter.formatDuration(Duration.ofSeconds(61)));
    assertEquals("01", compWriter.formatDuration(Duration.ofSeconds(1)));
  }

  private Competition createCompetitionWithTwoParticipants() {
    var comp = new Competition();
    var p1  = comp.addParticipant(new Participant("P 1"));
    var p2  = comp.addParticipant(new Participant("P 2"));
    comp.setTimingKeys("Round 1", "Mål");
    var start = LocalTime.of(11, 16, 0);
    p1.start(start);
    p2.start(start);
    var t21 = start
        .plus(1, ChronoUnit.HOURS)
        .plus(1, ChronoUnit.MINUTES)
        .plus(1, ChronoUnit.SECONDS);
    p1.time(t21, "Round 1");
    var t22 = t21
        .plus(1, ChronoUnit.SECONDS);
    p2.time(t22, "Round 1");
    var finish1 = t21
        .plus(1, ChronoUnit.MINUTES)
        .plus(1, ChronoUnit.SECONDS);
    p1.finish(finish1);
    var finish2 = t22
        .plus(1, ChronoUnit.MINUTES)
        .plus(1, ChronoUnit.SECONDS);
    p2.finish(finish2);    
    return comp;
  }

  @Test
  public void testWriteCompetitionCsvWithTwoParticipants() {
    assertEquals("""
        Participant,Start,Round 1,Mål
        P 1,11:16:00,01:01:01,01:02:02
        P 2,11:16:00,01:01:02,01:02:03
        """,
        CsvTarget.toString(createCompetitionWithTwoParticipants())
    );
  }

  @Test
  public void testWriteCompetitionHtmlTableWithTwoParticipants() {
    assertEquals("""
        <table>
          <tr><th>Participant</th><th>Start</th><th>Round 1</th><th>Mål</th></tr>
          <tr><td>P 1</td><td>11:16:00</td><td>01:01:01</td><td>01:02:02</td></tr>
          <tr><td>P 2</td><td>11:16:00</td><td>01:01:02</td><td>01:02:03</td></tr>
        </table>
        """,
        HtmlTableTarget.toString(createCompetitionWithTwoParticipants())
    );
  }

  @Test
  public void testWriteCompetitionKeepsTimingKeyOrderAndUsesFinishForTheLast() {
    var comp = new Competition();
    var p = comp.addParticipant(new Participant("P"));
    comp.setTimingKeys("1 km", "2 km", "3 km", "5 km", "8 km", "Mål");
    var start = LocalTime.of(10, 0, 0);
    p.start(start);
    p.time(start.plusMinutes(5), "5 km");
    p.finish(start.plusMinutes(11));
    assertEquals("""
        Participant,Start,1 km,2 km,3 km,5 km,8 km,Mål
        P,10:00:00,,,,05:00,,11:00
        """,
        CsvTarget.toString(comp)
    );
  }

  @Test
  public void testWriteCompetitionDoesNotWriteStop() {
    var comp = new Competition();
    var p = comp.addParticipant(new Participant("P"));
    comp.setTimingKeys("1 km", "Mål");
    var start = LocalTime.of(10, 0, 0);
    p.start(start);
    p.time(start.plusMinutes(5), "1 km");
    p.stop(start.plusMinutes(7));
    assertEquals("""
        Participant,Start,1 km,Mål
        P,10:00:00,05:00,
        """,
        CsvTarget.toString(comp)
    );
  }
}
