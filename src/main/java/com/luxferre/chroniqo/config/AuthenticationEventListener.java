package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.user.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Listens to Spring Security authentication events and delegates
 * to {@link LoginAttemptService} to track failed attempts and
 * clear counters on success.
 *
 * @author LuxFerre86
 * @since 11.03.2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;

    /**
     * Called after every successful authentication.
     * Resets the failed attempt counter for the authenticated user.
     */
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        loginAttemptService.recordSuccess(email);
    }

    /**
     * Called after every failed authentication attempt.
     * Increments the failure counter and locks the account if the
     * threshold is exceeded.
     */
    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String email = event.getAuthentication().getName();
        loginAttemptService.recordFailure(email);
        log.debug("Failed login attempt recorded for: [email hidden]");
    }
}