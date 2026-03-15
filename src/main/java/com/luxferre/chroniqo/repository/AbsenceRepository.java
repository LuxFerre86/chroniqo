package com.luxferre.chroniqo.repository;

import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Absence} entities.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
public interface AbsenceRepository extends JpaRepository<Absence, String> {

    List<Absence> findByUserAndDateBetween(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );

    Absence findByUserAndDate(
            User user,
            LocalDate date
    );
}