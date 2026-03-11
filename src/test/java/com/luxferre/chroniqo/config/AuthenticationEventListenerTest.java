package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.user.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationEventListenerTest {

    @InjectMocks
    private AuthenticationEventListener listener;

    @Mock
    private LoginAttemptService loginAttemptService;

    private static final String EMAIL = "user@example.com";

    private UsernamePasswordAuthenticationToken token() {
        return new UsernamePasswordAuthenticationToken(EMAIL, "password");
    }

    @Test
    void onSuccess_delegatesToRecordSuccess() {
        listener.onSuccess(new AuthenticationSuccessEvent(token()));

        verify(loginAttemptService).recordSuccess(EMAIL);
        verifyNoMoreInteractions(loginAttemptService);
    }

    @Test
    void onFailure_badCredentials_delegatesToRecordFailure() {
        listener.onFailure(new AuthenticationFailureBadCredentialsEvent(
                token(), new BadCredentialsException("bad credentials")));

        verify(loginAttemptService).recordFailure(EMAIL);
        verifyNoMoreInteractions(loginAttemptService);
    }

    @Test
    void onFailure_disabledAccount_isAlsoTracked() {
        // A disabled account (email not yet verified) still counts as a failure
        listener.onFailure(new AuthenticationFailureDisabledEvent(
                token(), new DisabledException("account disabled")));

        verify(loginAttemptService).recordFailure(EMAIL);
    }

    @Test
    void onFailure_lockedAccount_isAlsoTracked() {
        // Attempting to log into an already-locked account still records a failure
        listener.onFailure(new AuthenticationFailureLockedEvent(
                token(), new LockedException("account locked")));

        verify(loginAttemptService).recordFailure(EMAIL);
    }

    @Test
    void onSuccess_doesNotCallRecordFailure() {
        listener.onSuccess(new AuthenticationSuccessEvent(token()));

        verify(loginAttemptService, never()).recordFailure(any());
    }

    @Test
    void onFailure_doesNotCallRecordSuccess() {
        listener.onFailure(new AuthenticationFailureBadCredentialsEvent(
                token(), new BadCredentialsException("bad credentials")));

        verify(loginAttemptService, never()).recordSuccess(any());
    }
}