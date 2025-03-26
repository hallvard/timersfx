package no.hal.timers.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import no.hal.timers.core.Competition;
import no.hal.timers.core.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CompetitionCsvWriterTest {
  
    private CompetitionCsvWriter compCsvWriter;

  /**
   * Set up the CompetitionCsvWriter instance.
   */
  @BeforeEach
  public void setUp() {
    compCsvWriter = new CompetitionCsvWriter();
  }

  @Test
  public void testFormatDuration() {
    assertEquals("01:01:01", compCsvWriter.formatDuration(Duration.ofSeconds(3661)));
    assertEquals("01:01", compCsvWriter.formatDuration(Duration.ofSeconds(61)));
    assertEquals("01", compCsvWriter.formatDuration(Duration.ofSeconds(1)));
  }

  @Test
  public void testWriteCompetitionWithTwoParticipants() {
    var comp = new Competition();
    var p1  = comp.addParticipant(new Participant("P 1"));
    var p2  = comp.addParticipant(new Participant("P 2"));
    comp.setTimingKeys("Round 1", "STOP");
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
    var stop1 = t21
        .plus(1, ChronoUnit.MINUTES)
        .plus(1, ChronoUnit.SECONDS);
    p1.stop(stop1);
    var stop2 = t22
        .plus(1, ChronoUnit.MINUTES)
        .plus(1, ChronoUnit.SECONDS);
    p2.stop(stop2);
    assertEquals("""
        Participant,Round 1,STOP
        P 1,01:01:01,01:02:02
        P 2,01:01:02,01:02:03
        """,
        compCsvWriter.toString(comp)
    );
  }
}
