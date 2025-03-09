package no.hal.timers.core;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalTime;
import no.hal.timers.core.Participation.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParticipationTest {

  private Competition competition;
  private Participation participation;

  @BeforeEach
  public void setUp() {
    competition = new Competition();
    participation = new Participation(null);
  }

  @Test
  public void testInitialStatusIsInactive() {
    assertSame(Participation.Status.INACTIVE, participation.getStatus());
  }

  @Test
  public void testAfterSetCompetitionStatusIsStart() {
    participation.setCompetition(competition);
    assertSame(Status.START, participation.getStatus());
  }

  @Test
  public void testAfterStartStatusIsActive() {
    participation.start(LocalTime.now());
    assertSame(Status.ACTIVE, participation.getStatus());
  }

  @Test
  public void testAfterFinishStatusIsFinish() {
    participation.start(LocalTime.now());
    participation.finish(LocalTime.now());
    assertSame(Status.FINISH, participation.getStatus());
  }

  @Test
  public void testAfterStopStatusIsStop() {
    participation.start(LocalTime.now());
    participation.stop(LocalTime.now());
    assertSame(Status.STOP, participation.getStatus());
  }
}
