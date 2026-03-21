package com.luxferre.chroniqo.service.user;

import com.luxferre.chroniqo.config.AppProperties;
import com.luxferre.chroniqo.config.DefaultUserDetailsService;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.UserRepository;
import com.luxferre.chroniqo.service.EmailService;
import com.luxferre.chroniqo.service.RegistrationDisabledException;
import com.luxferre.chroniqo.service.event.UserChangedEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AppProperties appProperties;
    @Mock
    private AppProperties.RegistrationProperties registrationProperties;
    @Mock
    private UserDetails userDetails;

    // =========================================================================
    // register
    // =========================================================================

    @Nested
    class Register {

        @BeforeEach
        void setUp() {
            lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            lenient().when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
            lenient().when(appProperties.getRegistrationProperties()).thenReturn(registrationProperties);
            lenient().when(registrationProperties.isEnabled()).thenReturn(true);
        }

        @Test
        void register_newUser_savesUserAndSendsVerificationEmail() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

            User result = userService.register("new@example.com", "password123", "Max", "Mustermann");

            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getPasswordHash()).isEqualTo("hashed_password");
            assertThat(result.getFirstName()).isEqualTo("Max");
            assertThat(result.getLastName()).isEqualTo("Mustermann");
            assertThat(result.isEnabled()).isFalse();
            assertThat(result.getVerificationToken()).isNotNull();
            assertThat(result.getVerificationTokenExpiryDate()).isAfter(LocalDateTime.now());

            verify(userRepository).save(any(User.class));
            verify(emailService).sendVerificationEmail(any(User.class));
            // registration does not fire UserChangedEvent — user is not yet active
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void register_existingEmail_throwsIllegalArgumentException() {
            when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> userService.register("existing@example.com", "pw", "A", "B"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");

            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendVerificationEmail(any());
            verify(eventPublisher, never()).publishEvent(any());
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

        @AfterEach
        void clearContext() {
            SecurityContextHolder.clearContext();
            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        void verifyEmail_validToken_withRequestContext_returnsVerifiedLoggedIn() {
            User user = userWithVerificationToken("valid-token", LocalDateTime.now().plusHours(1));
            when(userRepository.findByVerificationToken("valid-token")).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userDetailsService.loadUserByUsername(any())).thenReturn(userDetails);
            when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession(true)).thenReturn(session);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            EmailVerificationResult result = userService.verifyEmail("valid-token");

            assertThat(result).isEqualTo(EmailVerificationResult.VERIFIED_LOGGED_IN);
            assertThat(user.isEnabled()).isTrue();
            assertThat(user.getVerificationToken()).isNull();
            assertThat(user.getVerificationTokenExpiryDate()).isNull();
        }

        @Test
        void verifyEmail_validToken_withRequestContext_persistsSecurityContextToSession() {
            User user = userWithVerificationToken("valid-token", LocalDateTime.now().plusHours(1));
            when(userRepository.findByVerificationToken("valid-token")).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userDetailsService.loadUserByUsername(any())).thenReturn(userDetails);
            when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession(true)).thenReturn(session);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            userService.verifyEmail("valid-token");

            verify(session).setAttribute(
                    eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any());
        }

        @Test
        void verifyEmail_validToken_withoutRequestContext_returnsVerifiedLoginRequired() {
            User user = userWithVerificationToken("valid-token", LocalDateTime.now().plusHours(1));
            when(userRepository.findByVerificationToken("valid-token")).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userDetailsService.loadUserByUsername(any())).thenReturn(userDetails);
            when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

            RequestContextHolder.resetRequestAttributes();

            EmailVerificationResult result = userService.verifyEmail("valid-token");

            assertThat(result).isEqualTo(EmailVerificationResult.VERIFIED_LOGIN_REQUIRED);
            assertThat(user.isEnabled()).isTrue();
            assertThat(user.getVerificationToken()).isNull();
        }

        @Test
        void verifyEmail_expiredToken_returnsInvalidAndDoesNotEnableUser() {
            User user = userWithVerificationToken("expired-token", LocalDateTime.now().minusHours(1));
            when(userRepository.findByVerificationToken("expired-token")).thenReturn(Optional.of(user));

            EmailVerificationResult result = userService.verifyEmail("expired-token");

            assertThat(result).isEqualTo(EmailVerificationResult.INVALID);
            assertThat(user.isEnabled()).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        void verifyEmail_unknownToken_returnsInvalid() {
            when(userRepository.findByVerificationToken("unknown")).thenReturn(Optional.empty());

            EmailVerificationResult result = userService.verifyEmail("unknown");

            assertThat(result).isEqualTo(EmailVerificationResult.INVALID);
            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // autoLogin
    // =========================================================================

    @Nested
    class AutoLogin {

        @AfterEach
        void cleanup() {
            SecurityContextHolder.clearContext();
            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        void autoLogin_withRequestContext_returnsTrue() {
            User user = new User();
            user.setEmail("user@example.com");
            user.setPasswordHash("hash");
            when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
            when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession(true)).thenReturn(session);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            assertThat(userService.autoLogin(user)).isTrue();
        }

        @Test
        void autoLogin_withRequestContext_persistsSecurityContextToSession() {
            User user = new User();
            user.setEmail("user@example.com");
            user.setPasswordHash("hash");
            when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
            when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession(true)).thenReturn(session);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            userService.autoLogin(user);

            verify(session).setAttribute(
                    eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any());
        }

        @Test
        void autoLogin_withRequestContext_setsAuthenticationInSecurityContextHolder() {
            User user = new User();
            user.setEmail("user@example.com");
            user.setPasswordHash("hash");
            when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
            when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession(true)).thenReturn(session);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            userService.autoLogin(user);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .isEqualTo(userDetails);
        }

        @Test
        void autoLogin_withoutRequestContext_returnsFalse() {
            User user = new User();
            user.setEmail("user@example.com");
            user.setPasswordHash("hash");
            when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
            when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

            RequestContextHolder.resetRequestAttributes();

            assertThat(userService.autoLogin(user)).isFalse();
        }

        @Test
        void autoLogin_withoutRequestContext_stillSetsSecurityContextHolder() {
            User user = new User();
            user.setEmail("user@example.com");
            user.setPasswordHash("hash");
            when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
            when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

            RequestContextHolder.resetRequestAttributes();

            userService.autoLogin(user);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
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

            assertThat(userService.resetPassword("expired-token", "newPassword")).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        void resetPassword_unknownToken_returnsFalse() {
            when(userRepository.findByResetToken("unknown")).thenReturn(Optional.empty());

            assertThat(userService.resetPassword("unknown", "newPassword")).isFalse();
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

            assertThatThrownBy(() ->
                    userService.changePassword("user@example.com", "wrongPassword", "newPassword"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("incorrect");

            verify(userRepository, never()).save(any());
        }

        @Test
        void changePassword_unknownUser_throwsIllegalArgumentException() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.changePassword("unknown@example.com", "pw", "newpw"))
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

            assertThat(user.getLastLoginAt()).isNotNull()
                    .isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        void updateLastLogin_unknownEmail_doesNothing() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            userService.updateLastLogin("unknown@example.com");

            verify(userRepository, never()).save(any());
        }
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
        void updateProfile_publishesUserChangedEvent() {
            User user = new User();
            user.setEmail("user@example.com");
            user.setFirstName("First");
            user.setLastName("Last");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            userService.updateProfile("user@example.com", "First", "Last", 40);

            verify(eventPublisher).publishEvent(any(UserChangedEvent.class));
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

            assertThatThrownBy(() ->
                    userService.updateProfile("user@example.com", "First", "Last", 999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("weeklyTargetHours must be between 0 and 80");

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void unknownEmail_throwsIllegalArgument() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.updateProfile("unknown@example.com", "First", "Last", 40))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    // =========================================================================
    // register – registration disabled
    // =========================================================================

    @Nested
    class RegisterWhenDisabled {

        @BeforeEach
        void disableRegistration() {
            lenient().when(appProperties.getRegistrationProperties()).thenReturn(registrationProperties);
            lenient().when(registrationProperties.isEnabled()).thenReturn(false);
        }

        @Test
        void register_throwsRegistrationDisabledException() {
            assertThatThrownBy(() ->
                    userService.register("new@example.com", "password123", "First", "Last"))
                    .isInstanceOf(RegistrationDisabledException.class)
                    .hasMessageContaining("disabled");
        }

        @Test
        void register_doesNotSaveUser() {
            try {
                userService.register("new@example.com", "password123", "First", "Last");
            } catch (RegistrationDisabledException ignored) {
            }

            verify(userRepository, never()).save(any());
        }

        @Test
        void register_doesNotSendEmail() {
            try {
                userService.register("new@example.com", "password123", "First", "Last");
            } catch (RegistrationDisabledException ignored) {
            }

            verify(emailService, never()).sendVerificationEmail(any());
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
}