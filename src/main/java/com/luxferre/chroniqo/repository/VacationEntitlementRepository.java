package com.luxferre.chroniqo.repository;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.model.VacationEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link VacationEntitlement} entities.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
public interface VacationEntitlementRepository extends JpaRepository<VacationEntitlement, String> {

    Optional<VacationEntitlement> findByUserAndYear(User user, int year);

    /**
     * Finds the most recent entitlement set for a year before the given one.
     */
    Optional<VacationEntitlement> findFirstByUserAndYearLessThanOrderByYearDesc(User user, int year);

    void deleteByUser(User user);
}
