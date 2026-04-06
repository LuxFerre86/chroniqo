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

    /**
     * Deletes all absence records associated with the specified user.
     *
     * @param user the user whose absence records should be deleted
     */
    void deleteByUser(User user);

    /**
     * Finds all absence records for a specific user within a date range.
     *
     * @param user the user to search for
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @return a list of absence records matching the criteria
     */
    List<Absence> findByUserAndDateBetween(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Finds a single absence record for a specific user on a given date.
     *
     * @param user the user to search for
     * @param date the date to search for
     * @return the absence record if found, null otherwise
     */
    Absence findByUserAndDate(
            User user,
            LocalDate date
    );
}