package com.luxferre.chroniqo.service.event;

/**
 * Functional interface for cancelling a previously registered listener.
 * Replaces the Vaadin {@code Registration} type so that the broadcaster
 * infrastructure works without the Vaadin dependency.
 *
 * @author Luxferre86
 */
@FunctionalInterface
public interface Registration {

    /**
     * Removes the registered listener, preventing it from receiving further
     * events.
     */
    void remove();
}
