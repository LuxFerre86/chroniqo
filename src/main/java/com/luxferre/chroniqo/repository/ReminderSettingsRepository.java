package com.luxferre.chroniqo.repository;

import com.luxferre.chroniqo.model.ReminderSettings;
import com.luxferre.chroniqo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ReminderSettings} entities.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
public interface ReminderSettingsRepository extends JpaRepository<ReminderSettings, String> {

    Optional<ReminderSettings> findByUser(User user);

    void deleteByUser(User user);
}
