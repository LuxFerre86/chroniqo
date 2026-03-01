package com.luxferre.chroniqo.repository;

import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, String> {

    List<Absence> findByUserAndDateLessThanEqualAndDateGreaterThanEqual(
            User user,
            LocalDate endDate,
            LocalDate startDate
    );

    Absence findByUserAndDate(
            User user,
            LocalDate date
    );
}
