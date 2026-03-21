package com.luxferre.chroniqo.service.event;

/**
 * Interface for components that listen to broadcast events.
 *
 * @author Luxferre86
 * @since 11.03.2026
 */
public interface BroadcastListener {

    /**
     * Called when a broadcast event occurs, specifically an entity change event.
     *
     * @param event the entity changed event that was broadcasted
     */
    void onBroadcastEvent(EntityChangedEvent event);
}
