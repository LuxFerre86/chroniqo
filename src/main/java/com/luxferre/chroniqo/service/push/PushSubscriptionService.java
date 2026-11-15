package com.luxferre.chroniqo.service.push;

import com.luxferre.chroniqo.i18n.Messages;
import com.luxferre.chroniqo.api.dto.ReminderSettingsDto;
import com.luxferre.chroniqo.model.PushSubscription;
import com.luxferre.chroniqo.model.ReminderSettings;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.PushSubscriptionRepository;
import com.luxferre.chroniqo.repository.ReminderSettingsRepository;
import com.luxferre.chroniqo.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Manages push subscriptions and reminder preferences of the current user.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    static final int MAX_SUBSCRIPTIONS_PER_USER = 20;

    private final PushSubscriptionRepository subscriptionRepository;
    private final ReminderSettingsRepository settingsRepository;
    private final UserService userService;

    /**
     * Registers or refreshes a device. An endpoint already known for another
     * user is reassigned, because it identifies the browser, not the account.
     */
    @Transactional
    public void subscribe(String endpoint, String p256dh, String auth) {
        User user = userService.getCurrentUser();
        PushSubscription subscription = subscriptionRepository.findByEndpoint(endpoint).orElse(null);
        if (subscription == null) {
            if (subscriptionRepository.findByUser(user).size() >= MAX_SUBSCRIPTIONS_PER_USER) {
                throw new IllegalArgumentException(Messages.get("error.push.tooManyDevices"));
            }
            subscription = new PushSubscription();
            subscription.setEndpoint(endpoint);
        }
        subscription.setUser(user);
        subscription.setP256dh(p256dh);
        subscription.setAuth(auth);
        subscriptionRepository.save(subscription);
    }

    /**
     * Removes one device of the current user.
     */
    @Transactional
    public void unsubscribe(String endpoint) {
        subscriptionRepository.deleteByUserAndEndpoint(userService.getCurrentUser(), endpoint);
    }

    /**
     * Returns the stored preferences or the defaults when none were saved yet.
     */
    public ReminderSettingsDto getSettings() {
        ReminderSettings settings = settingsRepository.findByUser(userService.getCurrentUser())
                .orElseGet(ReminderSettings::new);
        return toDto(settings);
    }

    /**
     * Saves the reminder preferences of the current user.
     */
    @Transactional
    public ReminderSettingsDto updateSettings(ReminderSettingsDto dto) {
        if (dto.endAfterHours() < 1 || dto.endAfterHours() > 24) {
            throw new IllegalArgumentException(Messages.get("error.push.endAfterHours"));
        }
        User user = userService.getCurrentUser();
        ReminderSettings settings = settingsRepository.findByUser(user).orElseGet(() -> {
            ReminderSettings created = new ReminderSettings();
            created.setUser(user);
            return created;
        });
        settings.setStartEnabled(dto.startEnabled());
        settings.setStartTime(dto.startTime().truncatedTo(ChronoUnit.MINUTES));
        settings.setEndEnabled(dto.endEnabled());
        settings.setEndAfterHours(dto.endAfterHours());
        return toDto(settingsRepository.save(settings));
    }

    private static ReminderSettingsDto toDto(ReminderSettings s) {
        LocalTime time = s.getStartTime();
        return new ReminderSettingsDto(s.isStartEnabled(), time, s.isEndEnabled(), s.getEndAfterHours());
    }
}
