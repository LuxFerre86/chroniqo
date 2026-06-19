package com.luxferre.chroniqo.repository;

import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for
 * {@link TimeEntry} entities.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
public interface TimeEntryRepository extends JpaRepository<TimeEntry, String> {

    /**
     * Finds all time entries for a user on a specific date sorted by start time.
     *
     * @param user the user to search for
     * @param date the date to search for
     * @return all matching entries sorted ascending by start time
     */
    List<TimeEntry> findByUserAndDateOrderByStartTimeAsc(User user, LocalDate date);

    /**
     * Finds a single time entry by id and user ownership.
     *
     * @param id   entry id
     * @param user current user
     * @return optional entry
     */
    Optional<TimeEntry> findByIdAndUser(String id, User user);

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

    /**
     * Finds all time entries for a user within a date range ordered by day and
     * start time.
     */
    List<TimeEntry> findByUserAndDateBetweenOrderByDateAscStartTimeAsc(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );
}