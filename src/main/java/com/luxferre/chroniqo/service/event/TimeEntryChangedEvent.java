package com.luxferre.chroniqo.service.event;

import com.luxferre.chroniqo.model.User;
import lombok.NonNull;

/**
 * Represents an event indicating that a time entry has changed.
 *
 * @author Luxferre86
 * @since 11.03.2026
 */
public class TimeEntryChangedEvent extends EntityChangedEvent {
    /**
     * Constructs a new TimeEntryChangedEvent.
     *
     * @param user     the user associated with the event
     * @param producer the class that produced the event
     */
    public TimeEntryChangedEvent(@NonNull User user, @NonNull Class<?> producer) {
        super(user, producer);
    }
}
