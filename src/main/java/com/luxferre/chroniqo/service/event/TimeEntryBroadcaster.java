package com.luxferre.chroniqo.service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * A broadcaster component for handling and broadcasting time entry changed events.
 *
 * @author Luxferre86
 * @since 11.03.2026
 */
@Slf4j
@Component
public class TimeEntryBroadcaster extends Broadcaster<TimeEntryChangedEvent> {

    /**
     * Handles the time entry changed event by broadcasting it.
     *
     * @param event the time entry changed event to broadcast
     */
    @Override
    public void handle(TimeEntryChangedEvent event) {
        broadcast(event);
    }
}
