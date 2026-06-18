package com.luxferre.chroniqo.service.user;

import com.luxferre.chroniqo.config.AppProperties;
import com.luxferre.chroniqo.config.DefaultUserDetailsService;
import com.luxferre.chroniqo.dto.UserRegistrationRequest;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.AbsenceRepository;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.repository.UserRepository;
import com.luxferre.chroniqo.service.EmailService;
import com.luxferre.chroniqo.service.RegistrationDisabledException;
import com.luxferre.chroniqo.service.event.UserChangedEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


/**
 * Core service for user account management: registration, email verification,
 * password management, and profile updates.
 *
 * <p>Registration creates an inactive account and sends a time-limited
 * verification email. Password reset follows the same token-based pattern with
 * a one-hour expiry. After successful email verification an auto-login is
 * attempted via {@link #autoLogin(User)}; if the HTTP session cannot be
 * established the caller receives
 * {@link EmailVerificationResult#VERIFIED_LOGIN_REQUIRED} and is expected to
 * redirect to the login page.
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final DefaultUserDetailsService userDetailsService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final AbsenceRepository absenceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AppProperties appProperties;

    /**
     * Returns the currently authenticated user by resolving the email from the
     * {@link SecurityContextHolder}.
     *
     * @return the authenticated {@link User}
     * @throws UserNotFoundException if no authenticated user can be determined
     */
    public User getCurrentUser() throws UserNotFoundException {
        return userDetailsService.getUsernameFromContext()
                .flatMap(userRepository::findByEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "Could not determine current user."));
    }

    /**
     * Registers a new user account.
     *
     * <p>Creates the account in a disabled state and sends an email-verification
     * message. The account is only activated once the user clicks the
     * verification link.
     *
     * @param request the registration request containing profile, password, and
     *                optional holiday-configuration fields
     * @return the persisted, not-yet-enabled {@link User}
     * @throws RegistrationDisabledException if registration is globally disabled
     * @throws IllegalArgumentException      if the email address is already
     *                                       registered
     */
    @Transactional
    public User register(UserRegistrationRequest request) {
        if (!appProperties.getRegistrationProperties().isEnabled()) {
            log.warn("User registration rejected: registration disabled");
            throw new RegistrationDisabledException(
                    "Registration is currently disabled.");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("User registration rejected: email already registered: {}", request.email());
            throw new IllegalArgumentException("Email already registered");
        }

        log.info("User registration initiated for email: {}", request.email());

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setWeeklyTargetHours(request.weeklyTargetHours());
        user.setCountryCode(request.countryCode());
        user.setSubdivisionCode(request.subdivisionCode());
        user.setEnabled(false);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiryDate(LocalDateTime.now().plusHours(24));

        user = userRepository.save(user);
        emailService.sendVerificationEmail(user);
        log.info("User registration completed for email: {}", request.email());
        return user;
    }

    /**
     * Resets the user's password if the supplied token is valid and not expired.
     *
     * @param token       the one-time reset token from the password-reset email
     * @param newPassword the plain-text replacement password
     * @return {@code true} on success; {@code false} if the token is unknown or
     *         expired
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByResetToken(token);
        if (userOpt.isEmpty()) {
            log.warn("Password reset failed: invalid token");
            return false;
        }

        User user = userOpt.get();
        if (user.getResetTokenExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Password reset failed: expired token");
            return false;
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiryDate(null);
        userRepository.save(user);
        log.info("Password reset successful");
        return true;
    }

    /**
     * Verifies the email address identified by {@code token} and attempts an
     * auto-login.
     *
     * @return {@link EmailVerificationResult#INVALID} if the token is unknown
     *         or expired, {@link EmailVerificationResult#VERIFIED_LOGGED_IN} if
     *         the session was established, or
     *         {@link EmailVerificationResult#VERIFIED_LOGIN_REQUIRED} if the
     *         token is valid but the session could not be persisted.
     */
    @Transactional
    public EmailVerificationResult verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);
        if (userOpt.isEmpty()) {
            log.warn("Email verification failed: invalid token");
            return EmailVerificationResult.INVALID;
        }

        User user = userOpt.get();
        if (user.getVerificationTokenExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Email verification failed: expired token");
            return EmailVerificationResult.INVALID;
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiryDate(null);
        userRepository.save(user);

        boolean sessionEstablished = autoLogin(user);
        log.info("Email verification successful");
        return sessionEstablished
                ? EmailVerificationResult.VERIFIED_LOGGED_IN
                : EmailVerificationResult.VERIFIED_LOGIN_REQUIRED;
    }

    /**
     * Generates a password-reset token and sends a reset-link email.
     *
     * <p>Silently does nothing when the email address is not registered, to
     * avoid leaking information about which addresses exist in the system.
     *
     * @param email the email address that requested a password reset
     */
    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();
        user.setResetToken(UUID.randomUUID().toString());
        user.setResetTokenExpiryDate(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        emailService.sendPasswordResetEmail(user);
        log.info("Password reset requested for email: {}", email);
    }

    /**
     * Updates the user's display name, weekly working-hour target, working
     * days, country, and subdivision for public holiday resolution.
     *
     * @param email             the email that identifies the user to update
     * @param firstName         the new first name
     * @param lastName          the new last name
     * @param weeklyTargetHours the new weekly target in hours (0–80)
     * @param workingDays       the new set of working days; must not be null or
     *                          empty
     * @param countryCode       ISO 3166-1 alpha-2 code; {@code null} disables
     *                          automatic holiday detection
     * @param subdivisionCode   full ISO 3166-2 code; {@code null} for
     *                          nationwide holidays only
     * @throws IllegalArgumentException if the user is not found,
     *         {@code weeklyTargetHours} is outside [0, 80], or
     *         {@code workingDays} is null or empty
     */
    @Transactional
    public void updateProfile(String email, String firstName, String lastName,
                              int weeklyTargetHours, Set<DayOfWeek> workingDays,
                              String countryCode, String subdivisionCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setWeeklyTargetHours(weeklyTargetHours);
        user.setWorkingDays(workingDays);
        user.setCountryCode(countryCode);
        user.setSubdivisionCode(subdivisionCode);
        userRepository.save(user);

        eventPublisher.publishEvent(new UserChangedEvent(user, getClass()));
        log.info("User profile updated");
    }

    /**
     * Changes the password for an already-authenticated user.
     *
     * @param email       the email identifying the account to update
     * @param oldPassword the current plain-text password for verification
     * @param newPassword the desired new plain-text password
     * @throws IllegalArgumentException if the user is not found or
     *         {@code oldPassword} does not match the stored hash
     */
    @Transactional
    public void changePassword(String email, String oldPassword,
                               String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("User password changed");
    }

    /**
     * Permanently deletes the currently authenticated user's account.
     *
     * <p>The current password must be supplied again to prevent accidental or
     * unauthorized self-deletion. Related time entries and absences are removed
     * first so the user entity can be deleted safely across all supported
     * database setups.
     *
     * @param currentPassword the user's current plain-text password
     * @throws IllegalArgumentException if the password does not match the
     *         authenticated user's stored password
     * @throws UserNotFoundException if no authenticated user can be resolved
     */
    @Transactional
    public void deleteCurrentUserAccount(String currentPassword) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        timeEntryRepository.deleteByUser(user);
        absenceRepository.deleteByUser(user);
        userRepository.delete(user);
        userRepository.flush();
        log.info("User account deleted");
    }

    /**
     * Records the current timestamp as the user's last-login time.
     * Silently does nothing when the email address is not found.
     *
     * @param email the email of the user who just authenticated
     */
    @Transactional
    public void updateLastLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
        log.info("User last login updated for email: {}", email);
    }

    /**
     * Auto-login after email verification.
     *
     * <p>Sets the authentication in the current thread's
     * {@link SecurityContextHolder} and, if a servlet request context is
     * available, persists it to the HTTP session.
     *
     * @return {@code true} if the session was successfully persisted;
     *         {@code false} if no request context was available
     */
    boolean autoLogin(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(
                user.getEmail());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails, user.getPasswordHash(),
                        userDetails.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        ServletRequestAttributes attr =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr != null) {
            HttpServletRequest request = attr.getRequest();
            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context);
            return true;
        }
        return false;
    }
}
