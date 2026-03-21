package com.luxferre.chroniqo.service.event;

import com.luxferre.chroniqo.model.User;
import lombok.NonNull;

/**
 * Event representing a change in an absence record.
 *
 * @author Luxferre86
 * @since 11.03.2026
 */
public class AbsenceChangedEvent extends EntityChangedEvent {
    /**
     * Constructs an AbsenceChangedEvent with the specified user and producer.
     *
     * @param user the user associated with the absence change
     * @param producer the class that produced this event
     */
    public AbsenceChangedEvent(@NonNull User user, @NonNull Class<?> producer) {
        super(user, producer);
    }
}
