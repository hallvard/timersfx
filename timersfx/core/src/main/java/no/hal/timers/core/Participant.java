package no.hal.timers.core;

/**
 * A participant in a competition.
 */
public class Participant {

  private String name;

  /**
   * Initialize a new participant with the given name.
   *
   * @param name the name
   */
  public Participant(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
