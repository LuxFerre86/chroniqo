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

    @Column(nullable = false)
    private boolean accountNonLocked = true;

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

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
