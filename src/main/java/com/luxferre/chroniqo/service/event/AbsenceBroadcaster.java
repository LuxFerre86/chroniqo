package com.luxferre.chroniqo.service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * Broadcaster responsible for handling and broadcasting absence-related events.
 *
 * @author Luxferre86
 * @since 11.03.2026
 */
@Slf4j
@Component
public class AbsenceBroadcaster extends Broadcaster<AbsenceChangedEvent> {

    /**
     * Handles the absence changed event by broadcasting it to registered listeners.
     *
     * @param event the absence changed event to be broadcasted
     */
    @Override
    public void handle(AbsenceChangedEvent event) {
        broadcast(event);
    }
}
