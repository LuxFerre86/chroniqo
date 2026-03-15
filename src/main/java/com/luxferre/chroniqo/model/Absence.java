package com.luxferre.chroniqo.model;

import com.luxferre.chroniqo.service.AbsenceService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * JPA entity representing a single day of absence for a user.
 *
 * <p>Each row covers exactly one calendar date. Multi-day absences are stored
 * as one row per working day — weekend days are excluded when an absence range
 * is saved via {@link AbsenceService}.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
@Getter
@Setter
@Entity
@Table(name = "absences")
@NoArgsConstructor
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private AbsenceType type;
}