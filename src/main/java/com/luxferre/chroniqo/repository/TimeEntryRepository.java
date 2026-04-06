package com.luxferre.chroniqo.repository;

import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for
 * {@link TimeEntry} entities.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
public interface TimeEntryRepository extends JpaRepository<TimeEntry, String> {

    /**
     * Finds a time entry by user and date.
     *
     * @param user the user to search for
     * @param date the date to search for
     * @return the time entry matching the user and date, or null if not found
     */
    TimeEntry findByUserAndDate(User user, LocalDate date);

    /**
     * Deletes all time entries for a specific user.
     *
     * @param user the user whose time entries should be deleted
     */
    void deleteByUser(User user);

    /**
     * Finds all time entries for a user within a date range.
     *
     * @param user the user to search for
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return a list of time entries within the specified date range
     */
    List<TimeEntry> findByUserAndDateBetween(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );
}