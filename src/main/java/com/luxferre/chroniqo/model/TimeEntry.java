package com.luxferre.chroniqo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JPA entity representing a user's time entry for a single working day.
 *
 * <p>An entry starts in {@link TimeEntryStatus#STARTED} state when only
 * {@code startTime} is known, and transitions to
 * {@link TimeEntryStatus#COMPLETED} once {@code endTime} is recorded.
 * Net working time is computed externally.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
@Getter
@Setter
@Entity
@Table(name = "time_entries")
@NoArgsConstructor
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakMinutes;

    /**
     * Status of this time entry
     * STARTED = Only start time set
     * COMPLETED = Start + End time set
     */
    @Enumerated(EnumType.STRING)
    private TimeEntryStatus status = TimeEntryStatus.COMPLETED;

    /**
     * Timestamp when entry was created/started
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when entry was completed
     */
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Check if entry is still active (not completed)
     */
    public boolean isActive() {
        return status == TimeEntryStatus.STARTED && endTime == null;
    }

    /**
     * Check if entry is complete
     */
    public boolean isComplete() {
        return status == TimeEntryStatus.COMPLETED &&
                startTime != null &&
                endTime != null;
    }
}