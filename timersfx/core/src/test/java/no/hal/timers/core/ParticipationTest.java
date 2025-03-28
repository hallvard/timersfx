package no.hal.timers.core;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalTime;
import no.hal.timers.core.Participation.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for Participation.
 */
public class ParticipationTest {

  private Competition competition;
  private Participant participant;
  private Participation participation;

  /**
   * Set up the Participation and Competion instances.
   */
  @BeforeEach
  public void setUp() {
    competition = new Competition();
    participant = new Participant("Hallvard");
    participation = competition.addParticipant(participant);
  }

  @Test
  public void testInitialStatusIsStart() {
    assertSame(Participation.Status.START, participation.getStatus());
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
