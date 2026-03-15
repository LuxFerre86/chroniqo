package com.luxferre.chroniqo.service.user;

import com.luxferre.chroniqo.config.AppProperties;
import com.luxferre.chroniqo.config.DefaultUserDetailsService;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.UserRepository;
import com.luxferre.chroniqo.service.EmailService;
import com.luxferre.chroniqo.service.RegistrationDisabledException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;


/**
 * Core service for user account management: registration, email verification,
 * password management, and profile updates.
 *
 * <p>Registration creates an inactive account and sends a time-limited
 * verification email. Password reset follows the same token-based pattern with
 * a one-hour expiry. After successful email verification an auto-login is
 * attempted via {@link #autoLogin(User)}; if the
 * HTTP session cannot be established the caller receives
 * {@link EmailVerificationResult#VERIFIED_LOGIN_REQUIRED}
 * and is expected to redirect to the login page.
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final DefaultUserDetailsService userDetailsService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;


    private final AppProperties appProperties;

    /**
     * Returns the currently authenticated user by resolving the email from the
     * {@link SecurityContextHolder}.
     *
     * @return the authenticated {@link User}
     * @throws UserNotFoundException if no authenticated user can be determined
     */
    public User getCurrentUser() throws UserNotFoundException {
        return userDetailsService.getUsernameFromContext().flatMap(userRepository::findByEmail).orElseThrow(() -> new UserNotFoundException("Could not determine current user."));
    }

    /**
     * Registers a new user account.
     *
     * <p>Creates the account in a disabled state and sends an email-verification
     * message. The account is only activated once the user clicks the verification
     * link.
     *
     * @param email     the unique email address for the new account
     * @param password  the plain-text password (will be hashed before storage)
     * @param firstName the user's first name
     * @param lastName  the user's last name
     * @return the persisted, not-yet-enabled {@link User}
     * @throws RegistrationDisabledException if registration is globally disabled
     * @throws IllegalArgumentException      if the email address is already registered
     */
    @Transactional
    public User register(String email, String password, String firstName, String lastName) {
        if (!appProperties.getRegistrationProperties().isEnabled()) {
            throw new RegistrationDisabledException("Registration is currently disabled.");
        }

        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Create new user
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(false); // Disabled until email verified

        // Generate verification token
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiryDate(LocalDateTime.now().plusHours(24));

        user = userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(user);

        return user;
    }

    /**
     * Resets the user's password if the supplied token is valid and not expired.
     *
     * @param token       the one-time reset token from the password-reset email
     * @param newPassword the plain-text replacement password
     * @return {@code true} on success; {@code false} if the token is unknown or
     * expired
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<User> userOpt = userRepository.findByResetToken(token);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Check if token is expired
        if (user.getResetTokenExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiryDate(null);
        userRepository.save(user);

        return true;
    }

    /**
     * Verify email with token and attempt auto-login.
     *
     * @return {@link EmailVerificationResult#INVALID} if the token is unknown or expired,
     * {@link EmailVerificationResult#VERIFIED_LOGGED_IN} if the token is valid and
     * the session was successfully established, or
     * {@link EmailVerificationResult#VERIFIED_LOGIN_REQUIRED} if the token is valid
     * but the session could not be persisted (user must log in manually).
     */
    @Transactional
    public EmailVerificationResult verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);

        if (userOpt.isEmpty()) {
            return EmailVerificationResult.INVALID;
        }

        User user = userOpt.get();

        // Check if token is expired
        if (user.getVerificationTokenExpiryDate().isBefore(LocalDateTime.now())) {
            return EmailVerificationResult.INVALID;
        }

        // Enable user
        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiryDate(null);
        userRepository.save(user);

        // auto login for user - may fail silently if no request context is available
        boolean sessionEstablished = autoLogin(user);
        return sessionEstablished
                ? EmailVerificationResult.VERIFIED_LOGGED_IN
                : EmailVerificationResult.VERIFIED_LOGIN_REQUIRED;
    }

    /**
     * Generates a password-reset token and sends a reset-link email.
     *
     * <p>Silently does nothing when the email address is not registered, to avoid
     * leaking information about which addresses exist in the system.
     *
     * @param email the email address that requested a password reset
     */
    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Don't reveal if email exists or not (security)
            return;
        }

        User user = userOpt.get();

        // Generate reset token
        user.setResetToken(UUID.randomUUID().toString());
        user.setResetTokenExpiryDate(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Send reset email
        emailService.sendPasswordResetEmail(user);
    }

    /**
     * Updates the user's display name and weekly working-hour target.
     *
     * @param email             the email that identifies the user to update
     * @param firstName         the new first name
     * @param lastName          the new last name
     * @param weeklyTargetHours the new weekly target in hours (0–80)
     * @throws IllegalArgumentException if the user is not found or
     *                                  {@code weeklyTargetHours} is outside [0, 80]
     */
    @Transactional
    public void updateProfile(String email, String firstName, String lastName, int weeklyTargetHours) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setWeeklyTargetHours(weeklyTargetHours);
        userRepository.save(user);
    }

    /**
     * Changes the password for an already-authenticated user.
     *
     * @param email       the email identifying the account to update
     * @param oldPassword the current plain-text password for verification
     * @param newPassword the desired new plain-text password
     * @throws IllegalArgumentException if the user is not found or
     *                                  {@code oldPassword} does not match the stored hash
     */
    @Transactional
    public void changePassword(String email, String oldPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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
    }

    /**
     * Auto-login after email verification.
     *
     * <p>Sets the authentication in the current thread's {@link SecurityContextHolder} and,
     * if a servlet request context is available, persists it to the HTTP session so that
     * subsequent requests are recognized as authenticated.
     *
     * @return {@code true} if the security context was successfully persisted to the HTTP
     * session; {@code false} if no request context was available (e.g. background
     * thread or Vaadin push context), in which case the caller should redirect the
     * user to the login page instead of assuming they are logged in.
     */
    boolean autoLogin(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails, user.getPasswordHash(), userDetails.getAuthorities()
                );
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
                    context
            );
            return true;
        }
        return false;
    }
}