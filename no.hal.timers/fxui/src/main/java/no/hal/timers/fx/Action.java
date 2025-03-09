package no.hal.timers.fx;

/**
 * An action that can be performed on an object of type T.
 */
public interface Action<T> {

  /**
   * Returns whether this action is for the given object.
   */
  boolean isFor(T t);

  /**
   * Performs this action on the given object.
   *
   * @param t the object to perform this action on
   */
  void doFor(T t);
}
