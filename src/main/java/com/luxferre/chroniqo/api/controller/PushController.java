package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.api.dto.PushSubscriptionRequest;
import com.luxferre.chroniqo.api.dto.ReminderSettingsDto;
import com.luxferre.chroniqo.service.push.PushSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints for Web Push subscriptions and reminder preferences.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushSubscriptionService pushService;

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@Valid @RequestBody PushSubscriptionRequest request) {
        pushService.subscribe(request.endpoint(), request.p256dh(), request.auth());
    }

    @DeleteMapping("/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@RequestParam String endpoint) {
        pushService.unsubscribe(endpoint);
    }

    @GetMapping("/settings")
    public ReminderSettingsDto settings() {
        return pushService.getSettings();
    }

    @PutMapping("/settings")
    public ReminderSettingsDto updateSettings(@Valid @RequestBody ReminderSettingsDto request) {
        return pushService.updateSettings(request);
    }
}
