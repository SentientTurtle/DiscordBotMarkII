package net.sentientturtle.discordbot.loader;

/**
 * Marker interface; All classes DIRECTLY implementing this interface will be statically initialized by {@link Loader}<br>
 * NOTE: Subclasses of a class implementing this interface are not loaded, and must themselves re-implement the interface.
 */
public interface StaticLoaded {}
