package com.luxferre.chroniqo.service.event;

import com.luxferre.chroniqo.model.User;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;


/**
 * Abstract broadcaster for handling entity change events.
 * Manages listeners for users and broadcasts events asynchronously using virtual threads.
 *
 * @param <E> the type of EntityChangedEvent this broadcaster handles
 * @author Luxferre86
 * @since 11.03.2026
 */
@Slf4j
abstract class Broadcaster<E extends EntityChangedEvent> {

    /**
     * Executor service using virtual threads for asynchronous broadcasting.
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Map of user emails to sets of broadcast listeners.
     */
    private final Map<String, Set<BroadcastListener>> userToListeners = new ConcurrentHashMap<>();

    /**
     * Registers a listener for a specific user.
     * If the user already has listeners, adds to the existing set; otherwise, creates a new set.
     *
     * @param user     the user to register the listener for
     * @param listener the listener to register
     * @return a Registration object to unregister the listener
     */
    public Registration register(User user, BroadcastListener listener) {
        userToListeners.computeIfAbsent(user.getEmail(),
                k -> ConcurrentHashMap.newKeySet()).add(listener);
        return () -> {
            Set<BroadcastListener> set = userToListeners.get(user.getEmail());
            if (set != null) set.remove(listener);
        };
    }

    /**
     * Handles the entity change event.
     * This method is invoked after the transaction is committed.
     * Subclasses must implement this method to define custom event handling logic.
     *
     * @param event the entity change event to handle
     */
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public abstract void handle(E event);

    /**
     * Broadcasts the event to all registered listeners for the event's user.
     * Executes each listener asynchronously using virtual threads.
     *
     * @param event the event to broadcast
     */
    void broadcast(E event) {
        Set<BroadcastListener> listeners = userToListeners.getOrDefault(
                event.getUser().getEmail(), Set.of());
        listeners.forEach(listener ->
                executor.execute(() -> listener.onBroadcastEvent(event)));
    }

    /**
     * Cleans up resources before the bean is destroyed.
     * Shuts down the executor service to prevent resource leaks.
     */
    @PreDestroy
    public void destroy() {
        executor.shutdown();
    }
}
