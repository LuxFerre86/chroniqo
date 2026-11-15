package com.luxferre.chroniqo.repository;

import com.luxferre.chroniqo.model.PushSubscription;
import com.luxferre.chroniqo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PushSubscription} entities.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, String> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByUser(User user);

    @Query("select distinct s.user from PushSubscription s")
    List<User> findSubscribedUsers();

    void deleteByUserAndEndpoint(User user, String endpoint);

    void deleteByUser(User user);
}
