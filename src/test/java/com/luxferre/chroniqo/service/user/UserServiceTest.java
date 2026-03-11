package com.luxferre.chroniqo.service.user;

import com.luxferre.chroniqo.config.DefaultUserDetailsService;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.UserRepository;
import com.luxferre.chroniqo.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private DefaultUserDetailsService userDetailsService;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;

    // =========================================================================
    // register
    // =========================================================================

    @Nested
    class Register {

        @BeforeEach
        void setUp() {
            lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        }

        @Test
        void register_newUser_savesUserAndSendsVerificationEmail() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

            User result = userService.register("new@example.com", "password123", "Max", "Mustermann");

            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getPasswordHash()).isEqualTo("hashed_password");
            assertThat(result.getFirstName()).isEqualTo("Max");
            assertThat(result.getLastName()).isEqualTo("Mustermann");
            assertThat(result.isEnabled()).isFalse(); // must not be active until email verified
            assertThat(result.getVerificationToken()).isNotNull();
            assertThat(result.getVerificationTokenExpiryDate()).isAfter(LocalDateTime.now());

            verify(userRepository).save(any(User.class));
            verify(emailService).sendVerificationEmail(any(User.class));
        }

        @Test
        void register_existingEmail_throwsIllegalArgumentException() {
            when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> userService.register("existing@example.com", "pw", "A", "B"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");

            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendVerificationEmail(any());
        }

        @Test
        void register_passwordIsHashed_notStoredInPlaintext() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            userService.register("user@example.com", "myPlainPassword", "A", "B");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("myPlainPassword");
        }
    }

    // =========================================================================
    // verifyEmail
    // =========================================================================

    @Nested
    class VerifyEmail {

        @Test
        void verifyEmail_validToken_enablesUserAndClearsToken() {
            User user = userWithVerificationToken("valid-token", LocalDateTime.now().plusHours(1));
            when(userRepository.findByVerificationToken("valid-token")).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            boolean result = userService.verifyEmail("valid-token");

            assertThat(result).isTrue();
            assertThat(user.isEnabled()).isTrue();
            assertThat(user.getVerificationToken()).isNull();
            assertThat(user.getVerificationTokenExpiryDate()).isNull();
        }

        @Test
        void verifyEmail_expiredToken_returnsFalseAndDoesNotEnableUser() {
            User user = userWithVerificationToken("expired-token", LocalDateTime.now().minusHours(1));
            when(userRepository.findByVerificationToken("expired-token")).thenReturn(Optional.of(user));

            boolean result = userService.verifyEmail("expired-token");

            assertThat(result).isFalse();
            assertThat(user.isEnabled()).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        void verifyEmail_unknownToken_returnsFalse() {
            when(userRepository.findByVerificationToken("unknown")).thenReturn(Optional.empty());

            boolean result = userService.verifyEmail("unknown");

            assertThat(result).isFalse();
            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // resetPassword
    // =========================================================================

    @Nested
    class ResetPassword {

        @Test
        void resetPassword_validToken_updatesPasswordAndClearsToken() {
            User user = userWithResetToken("valid-reset-token", LocalDateTime.now().plusMinutes(30));
            when(userRepository.findByResetToken("valid-reset-token")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newPassword")).thenReturn("hashed_new");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            boolean result = userService.resetPassword("valid-reset-token", "newPassword");

            assertThat(result).isTrue();
            assertThat(user.getPasswordHash()).isEqualTo("hashed_new");
            assertThat(user.getResetToken()).isNull();
            assertThat(user.getResetTokenExpiryDate()).isNull();
        }

        @Test
        void resetPassword_expiredToken_returnsFalse() {
            User user = userWithResetToken("expired-token", LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByResetToken("expired-token")).thenReturn(Optional.of(user));

            boolean result = userService.resetPassword("expired-token", "newPassword");

            assertThat(result).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        void resetPassword_unknownToken_returnsFalse() {
            when(userRepository.findByResetToken("unknown")).thenReturn(Optional.empty());

            boolean result = userService.resetPassword("unknown", "newPassword");

            assertThat(result).isFalse();
        }
    }

    // =========================================================================
    // changePassword
    // =========================================================================

    @Nested
    class ChangePassword {

        @Test
        void changePassword_correctOldPassword_updatesSuccessfully() {
            User user = userWithPassword();
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPassword", "old_hash")).thenReturn(true);
            when(passwordEncoder.encode("newPassword")).thenReturn("new_hash");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.changePassword("user@example.com", "oldPassword", "newPassword");

            assertThat(user.getPasswordHash()).isEqualTo("new_hash");
            verify(userRepository).save(user);
        }

        @Test
        void changePassword_wrongOldPassword_throwsIllegalArgumentException() {
            User user = userWithPassword();
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPassword", "old_hash")).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword("user@example.com", "wrongPassword", "newPassword"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("incorrect");

            verify(userRepository, never()).save(any());
        }

        @Test
        void changePassword_unknownUser_throwsIllegalArgumentException() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changePassword("unknown@example.com", "pw", "newpw"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // =========================================================================
    // requestPasswordReset
    // =========================================================================

    @Nested
    class RequestPasswordReset {

        @Test
        void requestPasswordReset_knownEmail_setsTokenAndSendsEmail() {
            User user = new User();
            user.setEmail("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.requestPasswordReset("user@example.com");

            assertThat(user.getResetToken()).isNotNull();
            assertThat(user.getResetTokenExpiryDate()).isAfter(LocalDateTime.now());
            verify(emailService).sendPasswordResetEmail(user);
        }

        @Test
        void requestPasswordReset_unknownEmail_doesNothingAndDoesNotRevealExistence() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            // Should not throw and should not send email
            userService.requestPasswordReset("unknown@example.com");

            verify(emailService, never()).sendPasswordResetEmail(any());
            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // updateLastLogin
    // =========================================================================

    @Nested
    class UpdateLastLogin {

        @Test
        void updateLastLogin_knownEmail_updatesTimestamp() {
            User user = new User();
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.updateLastLogin("user@example.com");

            assertThat(user.getLastLoginAt()).isNotNull();
            assertThat(user.getLastLoginAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        void updateLastLogin_unknownEmail_doesNothing() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            userService.updateLastLogin("unknown@example.com");

            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private User userWithVerificationToken(String token, LocalDateTime expiry) {
        User user = new User();
        user.setEmail("user@example.com");
        user.setEnabled(false);
        user.setVerificationToken(token);
        user.setVerificationTokenExpiryDate(expiry);
        return user;
    }

    private User userWithResetToken(String token, LocalDateTime expiry) {
        User user = new User();
        user.setEmail("user@example.com");
        user.setResetToken(token);
        user.setResetTokenExpiryDate(expiry);
        return user;
    }

    private User userWithPassword() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("old_hash");
        return user;
    }

    // =========================================================================
    // updateProfile
    // =========================================================================

    @Nested
    class UpdateProfile {

        @BeforeEach
        void setUp() {
            lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        void updatesNameAndWeeklyTargetHours() {
            User user = new User();
            user.setEmail("user@example.com");
            user.setFirstName("Old");
            user.setLastName("Name");
            user.setWeeklyTargetHours(0);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            userService.updateProfile("user@example.com", "New", "Name", 40);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getFirstName()).isEqualTo("New");
            assertThat(captor.getValue().getWeeklyTargetHours()).isEqualTo(40);
        }

        @Test
        void weeklyTargetHoursZero_accepted() {
            User user = new User();
            user.setEmail("user@example.com");
            user.setFirstName("First");
            user.setLastName("Last");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            userService.updateProfile("user@example.com", "First", "Last", 0);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getWeeklyTargetHours()).isZero();
        }

        @Test
        void invalidWeeklyTargetHours_throwsIllegalArgument() {
            User user = new User();
            user.setEmail("user@example.com");
            user.setFirstName("First");
            user.setLastName("Last");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.updateProfile("user@example.com", "First", "Last", 999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("weeklyTargetHours must be between 0 and 80");
        }

        @Test
        void unknownEmail_throwsIllegalArgument() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateProfile("unknown@example.com", "First", "Last", 40))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }


}