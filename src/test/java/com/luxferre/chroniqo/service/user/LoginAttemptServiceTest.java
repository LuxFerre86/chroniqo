package com.luxferre.chroniqo.service.user;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Mock
    private UserRepository userRepository;

    private static final String EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private User activeUser() {
        // lockedUntil is null by default → isAccountNonLocked() returns true
        return new User();
    }

    private User lockedUser() {
        User user = new User();
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        return user;
    }

    // =========================================================================
    // recordFailure
    // =========================================================================

    @Nested
    class RecordFailure {

        @Test
        void belowThreshold_doesNotPersistLock() {
            // 4 failures – one below threshold
            for (int i = 0; i < 4; i++) {
                loginAttemptService.recordFailure(EMAIL);
            }

            verify(userRepository, never()).save(any());
        }

        @Test
        void atThreshold_setsLockedUntilInDatabase() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));

            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                loginAttemptService.recordFailure(EMAIL);
            }

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            LocalDateTime lockedUntil = captor.getValue().getLockedUntil();
            assertThat(lockedUntil).isNotNull();
            assertThat(lockedUntil).isAfter(LocalDateTime.now());
            assertThat(lockedUntil).isBefore(LocalDateTime.now().plusMinutes(LoginAttemptService.LOCKOUT_MINUTES + 1));
        }

        @Test
        void lockedUntil_isApproximatelyNowPlusLockoutMinutes() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));

            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                loginAttemptService.recordFailure(EMAIL);
            }

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            LocalDateTime expected = LocalDateTime.now().plusMinutes(LoginAttemptService.LOCKOUT_MINUTES);
            LocalDateTime actual = captor.getValue().getLockedUntil();
            // Allow 5 seconds of clock skew in test execution
            assertThat(actual).isBetween(expected.minusSeconds(5), expected.plusSeconds(5));
        }

        @Test
        void aboveThreshold_savesOnlyOnce() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));

            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS + 3; i++) {
                loginAttemptService.recordFailure(EMAIL);
            }

            verify(userRepository, times(1)).save(any());
        }

        @Test
        void unknownEmail_doesNotThrow() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                loginAttemptService.recordFailure(EMAIL);
            }

            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // recordSuccess
    // =========================================================================

    @Nested
    class RecordSuccess {

        @Test
        void clearsInMemoryCounter_soSubsequentFailuresStartFresh() {
            // Build up 4 failures (below threshold)
            for (int i = 0; i < 4; i++) {
                loginAttemptService.recordFailure(EMAIL);
            }

            loginAttemptService.recordSuccess(EMAIL);

            // After success, another 4 failures should still not trigger a lock
            for (int i = 0; i < 4; i++) {
                loginAttemptService.recordFailure(EMAIL);
            }

            verify(userRepository, never()).save(any());
        }

        @Test
        void doesNotWriteToDatabase() {
            loginAttemptService.recordSuccess(EMAIL);

            verifyNoInteractions(userRepository);
        }
    }

    // =========================================================================
    // isBlocked
    // =========================================================================

    @Nested
    class IsBlocked {

        @Test
        void notBlocked_initially() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));

            assertThat(loginAttemptService.isBlocked(EMAIL)).isFalse();
        }

        @Test
        void blocked_afterReachingThreshold() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activeUser()));

            for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
                loginAttemptService.recordFailure(EMAIL);
            }

            // In-memory counter has reached threshold — blocked without DB call
            assertThat(loginAttemptService.isBlocked(EMAIL)).isTrue();
        }

        @Test
        void blocked_whenLockedUntilIsInFuture() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(lockedUser()));

            assertThat(loginAttemptService.isBlocked(EMAIL)).isTrue();
        }

        @Test
        void notBlocked_whenLockedUntilIsInPast() {
            User user = new User();
            user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

            assertThat(loginAttemptService.isBlocked(EMAIL)).isFalse();
        }

        @Test
        void notBlocked_forUnknownEmail() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThat(loginAttemptService.isBlocked(EMAIL)).isFalse();
        }
    }
}