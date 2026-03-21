package com.luxferre.chroniqo.service.event;

import com.luxferre.chroniqo.model.User;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Represents an event indicating that an entity has changed.
 * This event carries information about the user who triggered the change
 * and the producer class responsible for the change.
 *
 * @author Luxferre86
 * @since 11.03.2026
 */
@Getter
@ToString
@RequiredArgsConstructor
public class EntityChangedEvent {

    @NonNull
    private final User user;
    @NonNull
    private final Class<?> producer;
}
