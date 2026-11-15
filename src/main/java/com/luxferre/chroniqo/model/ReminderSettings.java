package com.luxferre.chroniqo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Reminder preferences of a user plus the bookkeeping that prevents
 * duplicate notifications.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@Getter
@Setter
@Entity
@Table(name = "reminder_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_reminder_settings_user", columnNames = "user_id"))
@NoArgsConstructor
public class ReminderSettings {

    public static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    public static final int DEFAULT_END_AFTER_HOURS = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_enabled", nullable = false)
    private boolean startEnabled = true;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime = DEFAULT_START_TIME;

    @Column(name = "end_enabled", nullable = false)
    private boolean endEnabled = true;

    @Column(name = "end_after_hours", nullable = false)
    private int endAfterHours = DEFAULT_END_AFTER_HOURS;

    @Column(name = "last_start_reminder_date")
    private LocalDate lastStartReminderDate;

    @Column(name = "last_end_reminder_entry_id", length = 36)
    private String lastEndReminderEntryId;
}
