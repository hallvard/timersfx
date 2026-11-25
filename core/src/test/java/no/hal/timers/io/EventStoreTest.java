package no.hal.timers.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import no.hal.timers.core.Competition;
import no.hal.timers.core.Participant;
import no.hal.timers.core.Participation.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class EventStoreTest {

  @TempDir
  Path directory;

  private EventStore store;

  @BeforeEach
  public void setUp() {
    store = new EventStore(directory.resolve("events"));
  }

  private Competition createCompetition() {
    var comp = new Competition();
    comp.setTimingKeys("3 km", "5 km", "Mål");
    var p1 = comp.addParticipant(new Participant("Hallvard, T"));
    var p2 = comp.addParticipant(new Participant("Kari \"K\""));
    var p3 = comp.addParticipant(new Participant("Ola"));
    var start = LocalTime.of(18, 30, 15);
    p1.start(start);
    p2.start(start);
    p1.time(start.plusMinutes(10), "3 km");
    p1.time(start.plusMinutes(17), "5 km");
    p1.finish(start.plusMinutes(25).plusSeconds(7));
    p2.time(start.plusMinutes(11), "3 km");
    return comp;
  }

  @Test
  public void testLocationRules() {
    assertTrue(EventStore.isValidLocation(""));
    assertTrue(EventStore.isValidLocation("storsvingen"));
    assertTrue(EventStore.isValidLocation("Å_ø-æ2"));
    assertFalse(EventStore.isValidLocation("stor svingen"));
    assertFalse(EventStore.isValidLocation("a/b"));
    assertFalse(EventStore.isValidLocation("a.b"));
    assertFalse(EventStore.isValidLocation(null));
  }

  @Test
  public void testSavedTableIsLoadedAsItWas() throws IOException {
    var name = store.create(LocalDate.of(2026, 11, 25), "storsvingen", createCompetition());
    assertEquals("2026-11-25-storsvingen", name);
    var loaded = store.load(name);

    assertEquals(List.of("3 km", "5 km", "Mål"), loaded.getTimingKeys());
    assertEquals(3, loaded.getParticipationCount());
    var p1 = loaded.getParticipation(0);
    var p2 = loaded.getParticipation(1);
    var p3 = loaded.getParticipation(2);
    assertEquals("Hallvard, T", p1.getParticipant().getName());
    assertEquals("Kari \"K\"", p2.getParticipant().getName());
    assertEquals(LocalTime.of(18, 30, 15), p1.getStartTime().get());
    assertEquals(Duration.ofMinutes(10), p1.getDuration("3 km").get());
    assertEquals(Duration.ofMinutes(17), p1.getDuration("5 km").get());
    assertEquals(Duration.ofMinutes(25).plusSeconds(7), p1.getDuration(Status.FINISH.name()).get());
    assertEquals(Status.FINISH, p1.getStatus());
    // started, with a lap but not finished, and not started
    assertEquals(Status.ACTIVE, p2.getStatus());
    assertEquals(Duration.ofMinutes(11), p2.getDuration("3 km").get());
    assertTrue(p2.getDuration("5 km").isEmpty());
    assertEquals(Status.START, p3.getStatus());
    assertTrue(p3.getStartTime().isEmpty());
  }

  @Test
  public void testFinishIsKeptWhateverTheLastKeyIsNamed() throws IOException {
    var comp = new Competition();
    comp.setTimingKeys("Finish");
    var p = comp.addParticipant(new Participant("A"));
    p.start(LocalTime.of(10, 0, 0));
    p.finish(LocalTime.of(10, 0, 0));
    var name = store.create(LocalDate.of(2026, 1, 1), "", comp);
    assertEquals("2026-01-01", name);
    assertEquals(Status.FINISH, store.load(name).getParticipation(0).getStatus());
  }

  @Test
  public void testSavingAgainReplacesTheTable() throws IOException {
    var comp = createCompetition();
    var name = store.create(LocalDate.of(2026, 11, 25), "a", comp);
    comp.getParticipation(1).finish(LocalTime.of(19, 0, 0));
    store.save(name, comp);
    assertEquals(Status.FINISH, store.load(name).getParticipation(1).getStatus());
    // only the two files are left in the directory
    try (var files = Files.list(directory.resolve("events"))) {
      assertEquals(2, files.count());
    }
  }

  @Test
  public void testListNewestFirstWithParticipantCount() throws IOException {
    store.create(LocalDate.of(2026, 11, 18), "a", createCompetition());
    store.create(LocalDate.of(2026, 11, 25), "b", new Competition() {{
      setTimingKeys("Mål");
    }});
    store.create(LocalDate.of(2026, 11, 25), "", createCompetition());
    var events = store.list();
    assertEquals(List.of("2026-11-25-b", "2026-11-25", "2026-11-18-a"),
        events.stream().map(EventStore.EventInfo::baseName).toList());
    assertEquals(List.of(0, 3, 3), events.stream().map(EventStore.EventInfo::participantCount).toList());
    assertEquals("b", events.get(0).location());
    assertEquals("", events.get(1).location());
    assertEquals(LocalDate.of(2026, 11, 25), events.get(0).date());
  }

  @Test
  public void testListOfNoEvents() throws IOException {
    assertTrue(store.list().isEmpty());
  }

  @Test
  public void testNameTaken() throws IOException {
    store.create(LocalDate.of(2026, 11, 25), "a", createCompetition());
    assertThrows(FileAlreadyExistsException.class,
        () -> store.create(LocalDate.of(2026, 11, 25), "a", createCompetition()));
    store.create(LocalDate.of(2026, 11, 25), "b", createCompetition());
    assertThrows(FileAlreadyExistsException.class, () -> store.rename("2026-11-25-a", "b"));
    assertEquals(2, store.list().size());
  }

  @Test
  public void testIllegalLocationIsRefused() {
    assertThrows(IllegalArgumentException.class,
        () -> store.create(LocalDate.of(2026, 11, 25), "../x", createCompetition()));
    assertThrows(IllegalArgumentException.class, () -> store.load("../x"));
    assertThrows(IllegalArgumentException.class, () -> store.delete("a/b"));
  }

  @Test
  public void testRenameChangesTheLocationAndTheFiles() throws IOException {
    var name = store.create(LocalDate.of(2026, 11, 25), "storsvingen", createCompetition());
    var newName = store.rename(name, "granasen");
    assertEquals("2026-11-25-granasen", newName);
    var events = store.list();
    assertEquals(1, events.size());
    assertEquals("granasen", events.get(0).location());
    assertEquals(3, store.load(newName).getParticipationCount());
    assertEquals(newName, store.rename(newName, "granasen"));
    assertEquals("2026-11-25", store.rename(newName, ""));
  }

  @Test
  public void testDuplicateKeepsParticipantsAndKeysButNotTimes() throws IOException {
    var name = store.create(LocalDate.of(2026, 11, 18), "a", createCompetition());
    var copyName = store.duplicate(name, LocalDate.of(2026, 11, 25), "a");
    assertEquals("2026-11-25-a", copyName);
    var copy = store.load(copyName);
    assertEquals(List.of("3 km", "5 km", "Mål"), copy.getTimingKeys());
    assertEquals(3, copy.getParticipationCount());
    assertEquals("Hallvard, T", copy.getParticipation(0).getParticipant().getName());
    assertTrue(copy.stream().allMatch(p -> p.getStatus() == Status.START));
    // the original is as it was
    assertEquals(Status.FINISH, store.load(name).getParticipation(0).getStatus());
  }

  @Test
  public void testDelete() throws IOException {
    var name = store.create(LocalDate.of(2026, 11, 25), "a", createCompetition());
    store.delete(name);
    assertTrue(store.list().isEmpty());
    // deleting what isn't there is not an error
    store.delete(name);
    // and the name can be used again
    store.create(LocalDate.of(2026, 11, 25), "a", createCompetition());
  }

  @Test
  public void testOldFormatWithoutStartIsStillRead() throws IOException {
    var comp = new CompetitionCsvReader().readCompetition(getClass().getResourceAsStream("sample1.csv"));
    assertEquals(6, comp.getParticipationCount());
    assertEquals(Duration.ofSeconds(52 * 60 + 54),
        comp.getParticipation(0).getDuration(Status.FINISH.name()).get());
  }
}
