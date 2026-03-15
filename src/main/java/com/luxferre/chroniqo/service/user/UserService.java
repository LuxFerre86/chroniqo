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


@Service
@RequiredArgsConstructor
public class UserService {

    private final DefaultUserDetailsService userDetailsService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;


    private final AppProperties appProperties;

    public User getCurrentUser() throws UserNotFoundException {
        return userDetailsService.getUsernameFromContext().flatMap(userRepository::findByEmail).orElseThrow(() -> new UserNotFoundException("Could not determine current user."));
    }

    /**
     * Register a new user
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
     * Request password reset
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
     * Update user profile
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
     * Change password (when user is logged in)
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
     * Update last login time
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
     * subsequent requests are recognised as authenticated.
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