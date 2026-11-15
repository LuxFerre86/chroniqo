package com.luxferre.chroniqo.service.push;

import com.luxferre.chroniqo.i18n.Messages;
import com.luxferre.chroniqo.model.*;
import com.luxferre.chroniqo.repository.AbsenceRepository;
import com.luxferre.chroniqo.repository.PushSubscriptionRepository;
import com.luxferre.chroniqo.repository.ReminderSettingsRepository;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.service.CustomHolidayService;
import com.luxferre.chroniqo.service.PublicHolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Decides which users get a start- or end-of-work reminder and sends it to all
 * of their registered devices.
 *
 * <p>The start reminder is sent once per day within a catch-up window after the
 * configured time, so a short outage does not swallow it. The end reminder is
 * sent once per open entry. Times follow the server time zone.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    static final int START_WINDOW_HOURS = 3;
    private static final String TARGET_URL = "/";

    private final PushSender pushSender;
    private final PushSubscriptionRepository subscriptionRepository;
    private final ReminderSettingsRepository settingsRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final AbsenceRepository absenceRepository;
    private final PublicHolidayService publicHolidayService;
    private final CustomHolidayService customHolidayService;

    /**
     * Evaluates all subscribed users for the given moment.
     */
    @Transactional
    public void process(LocalDateTime now) {
        if (!pushSender.isEnabled()) {
            return;
        }
        for (User user : subscriptionRepository.findSubscribedUsers()) {
            try {
                processUser(user, now);
            } catch (RuntimeException e) {
                log.warn("Reminder processing failed for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    private void processUser(User user, LocalDateTime now) {
        ReminderSettings settings = settingsRepository.findByUser(user).orElseGet(() -> {
            ReminderSettings created = new ReminderSettings();
            created.setUser(user);
            return created;
        });
        boolean changed = false;
        LocalDate today = now.toLocalDate();

        if (settings.isStartEnabled() && isStartReminderDue(user, settings, now)) {
            deliver(user, Messages.forLanguage(user.getLanguage(), "push.title"),
                    Messages.forLanguage(user.getLanguage(), "push.start.body"));
            settings.setLastStartReminderDate(today);
            changed = true;
        }

        if (settings.isEndEnabled()) {
            for (TimeEntry entry : timeEntryRepository.findByUserAndStatus(user, TimeEntryStatus.STARTED)) {
                LocalDateTime started = LocalDateTime.of(entry.getDate(), entry.getStartTime());
                if (!now.isBefore(started.plusHours(settings.getEndAfterHours()))
                        && !entry.getId().equals(settings.getLastEndReminderEntryId())) {
                    deliver(user, Messages.forLanguage(user.getLanguage(), "push.title"),
                            Messages.forLanguage(user.getLanguage(), "push.end.body", settings.getEndAfterHours()));
                    settings.setLastEndReminderEntryId(entry.getId());
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            settingsRepository.save(settings);
        }
    }

    private boolean isStartReminderDue(User user, ReminderSettings settings, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        if (today.equals(settings.getLastStartReminderDate())) {
            return false;
        }
        LocalDateTime windowStart = LocalDateTime.of(today, settings.getStartTime());
        if (now.isBefore(windowStart) || !now.isBefore(windowStart.plusHours(START_WINDOW_HOURS))) {
            return false;
        }
        if (!user.getWorkingDaysOrDefault().contains(today.getDayOfWeek())
                || publicHolidayService.isHoliday(today, user.getCountryCode(), user.getSubdivisionCode())
                || customHolidayService.getDays(user, today, today).containsKey(today)
                || !absenceRepository.findByUserAndDateBetween(user, today, today).isEmpty()) {
            return false;
        }
        return timeEntryRepository.findByUserAndDateOrderByStartTimeAsc(user, today).isEmpty();
    }

    private void deliver(User user, String title, String body) {
        String payload = "{\"title\":\"" + escape(title) + "\",\"body\":\"" + escape(body)
                + "\",\"url\":\"" + TARGET_URL + "\"}";
        List<PushSubscription> subscriptions = subscriptionRepository.findByUser(user);
        for (PushSubscription subscription : subscriptions) {
            if (pushSender.send(subscription, payload) == PushSender.Result.GONE) {
                subscriptionRepository.delete(subscription);
            }
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
