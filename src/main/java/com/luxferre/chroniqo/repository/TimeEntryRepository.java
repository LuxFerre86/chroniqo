package com.luxferre.chroniqo.repository;

import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, String> {

    TimeEntry findByUserAndDate(User user, LocalDate date);

    List<TimeEntry> findByUserAndDateBetween(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );
}
