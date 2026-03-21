package com.luxferre.chroniqo.service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * Broadcaster for user-related events.
 *
 * @author Luxferre86
 * @since 11.03.2026
 */
@Slf4j
@Component
public class UserBroadcaster extends Broadcaster<UserChangedEvent> {

    /**
     * Handles the user changed event by broadcasting it.
     *
     * @param event the user changed event
     */
    @Override
    public void handle(UserChangedEvent event) {
        broadcast(event);
    }
}
