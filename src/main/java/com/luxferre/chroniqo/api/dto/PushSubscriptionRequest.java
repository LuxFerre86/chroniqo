package com.luxferre.chroniqo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Browser push subscription as delivered by {@code PushSubscription.toJSON()}.
 */
public record PushSubscriptionRequest(@NotBlank @Size(max = 700) String endpoint,
                                      @NotBlank @Size(max = 255) String p256dh,
                                      @NotBlank @Size(max = 255) String auth) {
}
