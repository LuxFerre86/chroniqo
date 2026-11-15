package com.luxferre.chroniqo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A user-defined day off (bridge day, company holiday) that behaves like a
 * public holiday. Covers {@code startDate} to {@code endDate} inclusive; a
 * recurring entry repeats every year on the same month and day.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@Getter
@Setter
@Entity
@Table(name = "custom_holidays",
        indexes = @Index(name = "idx_custom_holidays_user", columnList = "user_id"))
@NoArgsConstructor
public class CustomHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean recurring;
}
