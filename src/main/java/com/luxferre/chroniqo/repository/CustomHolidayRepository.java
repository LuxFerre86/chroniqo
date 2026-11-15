package com.luxferre.chroniqo.repository;

import com.luxferre.chroniqo.model.CustomHoliday;
import com.luxferre.chroniqo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link CustomHoliday} entities.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
public interface CustomHolidayRepository extends JpaRepository<CustomHoliday, String> {

    List<CustomHoliday> findByUserOrderByStartDateAsc(User user);

    Optional<CustomHoliday> findByIdAndUser(String id, User user);

    void deleteByUser(User user);
}
