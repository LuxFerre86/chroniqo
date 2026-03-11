package com.luxferre.chroniqo.service.user;

import com.luxferre.chroniqo.config.DefaultUserDetailsService;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.UserRepository;
import com.luxferre.chroniqo.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public User getCurrentUser() throws UserNotFoundException {
        return userDetailsService.getUsernameFromContext().flatMap(userRepository::findByEmail).orElseThrow(() -> new UserNotFoundException("Could not determine current user."));
    }

    /**
     * Register a new user
     */
    @Transactional
    public User register(String email, String password, String firstName, String lastName) {
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
     * Verify email with token
     */
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<User> userOpt = userRepository.findByVerificationToken(token);

        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        // Check if token is expired
        if (user.getVerificationTokenExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Enable user
        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiryDate(null);
        userRepository.save(user);

        return true;
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
     * Auto-login after registration
     */
    public void autoLogin(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}