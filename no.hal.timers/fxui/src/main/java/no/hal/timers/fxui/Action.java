package no.hal.timers.fxui;

public interface Action<T> {

	public boolean isFor(T t);
	public void doFor(T t);

}
