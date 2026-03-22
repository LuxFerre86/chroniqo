package com.luxferre.chroniqo.model;

import com.luxferre.chroniqo.config.DefaultUserDetailsService;
import com.luxferre.chroniqo.service.user.LoginAttemptService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JPA entity representing an application user.
 *
 * <p>Authentication state is controlled by {@code enabled} (set to
 * {@code true} after email verification) and {@code lockedUntil} (set by
 * {@link LoginAttemptService} after too many failed login attempts). Both flags
 * are evaluated by {@link DefaultUserDetailsService} on every authentication
 * request.
 *
 * <p>The set of configured working days ({@code workingDays}) determines on
 * which days of the week the user is expected to work. It defaults to Monday
 * through Friday and is used by
 * {@link com.luxferre.chroniqo.service.SummaryService} to compute daily targets
 * and the running time balance.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
@Getter
@Setter
@Entity
@Table(name = "users")
@NoArgsConstructor
public class User {

    /**
     * Default working days: Monday through Friday.
     */
    public static final Set<DayOfWeek> DEFAULT_WORKING_DAYS =
            EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private int weeklyTargetHours;

    /**
     * Sets the user's weekly working-hour target.
     *
     * @param weeklyTargetHours the target in whole hours; must be in [0, 80]
     * @throws IllegalArgumentException if the value is outside [0, 80]
     */
    public void setWeeklyTargetHours(int weeklyTargetHours) {
        if (weeklyTargetHours < 0 || weeklyTargetHours > 80) {
            throw new IllegalArgumentException(
                    "weeklyTargetHours must be between 0 and 80, got: " + weeklyTargetHours);
        }
        this.weeklyTargetHours = weeklyTargetHours;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_workdays", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "workday")
    @Enumerated(EnumType.STRING)
    private List<DayOfWeek> workingDays;

    private String federalState;

    // ===== Authentication Fields =====

    @Column(nullable = false)
    private boolean enabled = false;

    /**
     * If set, the account is locked until this timestamp.
     * Null means the account is not locked.
     */
    private LocalDateTime lockedUntil;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    // Password Reset
    private String resetToken;
    private LocalDateTime resetTokenExpiryDate;

    // Email Verification
    private String verificationToken;
    private LocalDateTime verificationTokenExpiryDate;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Returns the user's configured working days as an {@link EnumSet}, falling
     * back to {@link #DEFAULT_WORKING_DAYS} (Monday–Friday) when none have been
     * configured yet.
     *
     * @return non-null, non-empty set of working days
     */
    public Set<DayOfWeek> getWorkingDaysOrDefault() {
        if (workingDays == null || workingDays.isEmpty()) {
            return DEFAULT_WORKING_DAYS;
        }
        return EnumSet.copyOf(workingDays);
    }

    /**
     * Replaces the user's working days with the given set.
     *
     * @param days the new working days; must not be null or empty
     * @throws IllegalArgumentException if {@code days} is null or empty
     */
    public void setWorkingDays(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            throw new IllegalArgumentException("At least one working day must be configured");
        }
        this.workingDays = days.stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Returns true if the account is currently locked.
     * A lock expires automatically once {@code lockedUntil} is in the past.
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && !lockedUntil.isBefore(LocalDateTime.now());
    }

    /**
     * Returns the user's full display name as "{@code firstName lastName}".
     *
     * @return first and last name separated by a single space
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}