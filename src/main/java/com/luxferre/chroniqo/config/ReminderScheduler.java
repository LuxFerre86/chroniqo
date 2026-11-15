package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.push.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Triggers the reminder evaluation every minute.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;
    private final Clock clock;

    @Scheduled(cron = "0 * * * * *")
    public void run() {
        reminderService.process(LocalDateTime.now(clock));
    }
}
