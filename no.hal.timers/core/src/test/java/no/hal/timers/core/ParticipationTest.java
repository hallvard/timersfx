package no.hal.timers.core;

import java.time.LocalTime;

import org.junit.jupiter.api.Assertions;
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
		Assertions.assertSame(Participation.Status.INACTIVE, participation.getStatus());
	}

	@Test
	public void testAfterSetCompetitionStatusIsStart() {
		participation.setCompetition(competition);
		Assertions.assertSame(Participation.Status.START, participation.getStatus());
	}

	@Test
	public void testAfterStartStatusIsActive() {
		participation.start(LocalTime.now());
		Assertions.assertSame(Participation.Status.ACTIVE, participation.getStatus());
	}

	@Test
	public void testAfterFinishStatusIsFinish() {
		participation.start(LocalTime.now());
		participation.finish(LocalTime.now());
		Assertions.assertSame(Participation.Status.FINISH, participation.getStatus());
	}

	@Test
	public void testAfterStopStatusIsStop() {
		participation.start(LocalTime.now());
		participation.stop(LocalTime.now());
		Assertions.assertSame(Participation.Status.STOP, participation.getStatus());
	}
}
