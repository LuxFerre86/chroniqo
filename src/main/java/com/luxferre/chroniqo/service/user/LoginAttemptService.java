package com.luxferre.chroniqo.service.user;

import com.luxferre.chroniqo.config.DefaultUserDetailsService;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks failed login attempts per email address and temporarily locks
 * accounts after exceeding the maximum number of allowed attempts.
 *
 * <p>The lock expiry time is persisted in the database via {@code User.lockedUntil},
 * so it survives application restarts and is evaluated automatically by
 * {@link DefaultUserDetailsService} on every authentication attempt —
 * no background scheduler or manual unlock call required.
 *
 * <p>The in-memory attempt counter is used to detect the threshold before
 * writing to the database. It resets automatically once the lockout window expires.
 *
 * @author LuxFerre86
 * @since 11.03.2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;

    /**
     * In-memory attempt counter: email -> attempt record
     */
    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /**
     * Records a failed login attempt for the given email address.
     * Once the threshold is reached, sets {@code lockedUntil} on the user
     * in the database to the current time plus the lockout duration.
     *
     * @param email the email address that failed to authenticate
     */
    @Transactional
    public void recordFailure(String email) {
        AttemptRecord record = attempts.compute(email, (key, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new AttemptRecord(1, LocalDateTime.now());
            }
            return existing.increment();
        });

        if (record.count() == MAX_ATTEMPTS) {
            LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES);
            userRepository.findByEmail(email).ifPresent(user -> {
                user.setLockedUntil(lockedUntil);
                userRepository.save(user);
                log.warn("Account locked until {} due to too many failed login attempts: [email hidden]", lockedUntil);
            });
        }
    }

    /**
     * Clears the in-memory failed attempt counter for the given email
     * after a successful login.
     *
     * @param email the email address that successfully authenticated
     */
    public void recordSuccess(String email) {
        attempts.remove(email);
    }

    /**
     * Returns whether the given email address is currently blocked from logging in,
     * based on the in-memory attempt counter or the persisted {@code lockedUntil} timestamp.
     *
     * <p>Note: the persisted lock is authoritative — it is evaluated by Spring Security
     * directly via {@link DefaultUserDetailsService#loadUserByUsername}, so this method
     * is primarily useful for pre-checks in the UI layer.
     *
     * @param email the email address to check
     * @return true if the account is currently locked
     */
    public boolean isBlocked(String email) {
        AttemptRecord record = attempts.get(email);
        if (record != null && !record.isExpired() && record.count() >= MAX_ATTEMPTS) {
            return true;
        }
        return userRepository.findByEmail(email)
                .map(User::isAccountLocked)
                .orElse(false);
    }

    private record AttemptRecord(int count, LocalDateTime firstAttempt) {

        boolean isExpired() {
            return firstAttempt.plusMinutes(LOCKOUT_MINUTES).isBefore(LocalDateTime.now());
        }

        AttemptRecord increment() {
            return new AttemptRecord(count + 1, firstAttempt);
        }
    }
}