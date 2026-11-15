package com.luxferre.chroniqo.service.push;

import com.luxferre.chroniqo.model.PushSubscription;

/**
 * Delivers a payload to a registered push endpoint.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
public interface PushSender {

    /**
     * Outcome of a delivery attempt.
     */
    enum Result {
        /** The push service accepted the message. */
        SENT,
        /** The subscription no longer exists and should be removed. */
        GONE,
        /** Delivery failed for another, possibly temporary, reason. */
        FAILED
    }

    /**
     * Whether a VAPID key pair is configured so that messages can be sent.
     */
    boolean isEnabled();

    /**
     * Sends a JSON payload to the given subscription.
     */
    Result send(PushSubscription subscription, String jsonPayload);
}
