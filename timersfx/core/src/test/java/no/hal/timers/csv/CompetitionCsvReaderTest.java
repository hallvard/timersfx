package no.hal.timers.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import no.hal.timers.core.Participation;
import no.hal.timers.core.Participation.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for CompetitionCsvReader.
 */
public class CompetitionCsvReaderTest {

  private CompetitionCsvReader compCsvReader;

  /**
   * Set up the CompetitionCsvReader instance.
   */
  @BeforeEach
  public void setUp() {
    compCsvReader = new CompetitionCsvReader();
  }

  @Test
  public void testParseDuration() {
    assertEquals(Duration.of(40, ChronoUnit.SECONDS),
        compCsvReader.parseDuration("40"));
    assertEquals(Duration.of(10 * 60 + 40, ChronoUnit.SECONDS),
        compCsvReader.parseDuration("10:40"));
    assertEquals(Duration.of(1 * 60 * 60 + 10 * 60 + 40, ChronoUnit.SECONDS),
        compCsvReader.parseDuration("1:10:40"));
  }

  @Test
  public void testSample1() throws Exception {
    var competition = compCsvReader.readCompetition(getClass().getResourceAsStream("sample1.csv"));

    var expectedTimingKeys = Arrays.asList("1.runde", "2.runde", "3.runde", "4.runde", "FINISH");
    assertEquals(expectedTimingKeys.size(), competition.getTimingKeys().size());
    assertTrue(competition.getTimingKeys().containsAll(expectedTimingKeys));

    assertEquals(6, competition.getParticipationCount());
    var participations = competition.iterator();

    var p1 = participations.next();
    assertEquals(Duration.ofSeconds(13 * 60 + 00), p1.getDuration("1.runde").get());
    assertEquals(Duration.ofSeconds(26 * 60 + 14), p1.getDuration("2.runde").get());
    assertEquals(Duration.ofSeconds(39 * 60 + 35), p1.getDuration("3.runde").get());
    assertEquals(Duration.ofSeconds(52 * 60 + 54), p1.getDuration("4.runde").get());
    assertEquals(Duration.ofSeconds(52 * 60 + 54), p1.getDuration(Status.FINISH.name()).get());

    participations.next();
    participations.next();
    participations.next();
    participations.next();

    final Participation p6 = participations.next();
    assertEquals(Duration.ofSeconds(11 * 60 + 30), p6.getDuration("1.runde").get());
    assertEquals(Duration.ofSeconds(22 * 60 + 58), p6.getDuration("2.runde").get());
    assertEquals(Duration.ofSeconds(34 * 60 + 23), p6.getDuration("3.runde").get());
    assertEquals(Duration.ofSeconds(45 * 60 + 39), p6.getDuration("4.runde").get());
    assertEquals(Duration.ofSeconds(45 * 60 + 39), p6.getDuration(Status.FINISH.name()).get());
  }
}
