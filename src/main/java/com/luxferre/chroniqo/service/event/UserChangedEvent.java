package com.luxferre.chroniqo.service.event;

import com.luxferre.chroniqo.model.User;
import lombok.NonNull;

/**
 * Event triggered when a user entity is changed.
 *
 * @author Luxferre86
 * @since 11.03.2026
 */
public class UserChangedEvent extends EntityChangedEvent {

    /**
     * Constructs a UserChangedEvent with the specified user and producer.
     *
     * @param user     the user entity that was changed, must not be null
     * @param producer the class that produced this event, must not be null
     */
    public UserChangedEvent(@NonNull User user, @NonNull Class<?> producer) {
        super(user, producer);
    }
}
