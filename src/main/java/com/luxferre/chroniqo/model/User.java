package com.luxferre.chroniqo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
@NoArgsConstructor
public class User {

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

    @ElementCollection
    @CollectionTable(name = "user_workdays", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "workday")
    private List<String> workingDays; // MONDAY, TUESDAY, ...

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
     * Returns true if the account is currently not locked.
     * A lock expires automatically once {@code lockedUntil} is in the past.
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && !lockedUntil.isBefore(LocalDateTime.now());
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}