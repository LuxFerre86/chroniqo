package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.user.UserService;
import com.luxferre.chroniqo.util.LoggingTestUtils;
import ch.qos.logback.classic.Level;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LastLoginTokenBasedRememberMeServicesTest {

    @Mock
    private UserService userService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private User user;

    @Mock
    private UserDetails userDetails;

    private LastLoginTokenBasedRememberMeServices rememberMeServices;
    private LoggingTestUtils logs;

    @BeforeEach
    void setUp() {
        logs = LoggingTestUtils.captureLogsFor(LastLoginTokenBasedRememberMeServices.class);
        rememberMeServices = new LastLoginTokenBasedRememberMeServices(
                "test-key", userDetailsService, userService,
                TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256);

        when(userDetailsService.loadUserByUsername(any())).thenReturn(userDetails);
    }

    @AfterEach
    void tearDown() {
        logs.stop();
    }

    @Test
    void onLoginSuccess_delegatesUpdateLastLoginToUserService() {
        when(authentication.getPrincipal()).thenReturn(user);
        when(authentication.getName()).thenReturn("user@example.com");

        rememberMeServices.onLoginSuccess(request, response, authentication);

        verify(userService).updateLastLogin("user@example.com");
    }

    @Test
    void onLoginSuccess_usesAuthenticationNameAsEmailIdentifier() {
        when(authentication.getPrincipal()).thenReturn(user);
        when(authentication.getName()).thenReturn("other@example.com");

        rememberMeServices.onLoginSuccess(request, response, authentication);

        verify(userService).updateLastLogin("other@example.com");
        verify(userService, never()).updateLastLogin("user@example.com");
    }

    @Test
    void onLoginSuccess_logsSuccessfulTokenBasedAuthentication() {
        when(authentication.getPrincipal()).thenReturn(user);
        when(authentication.getName()).thenReturn("user@example.com");

        rememberMeServices.onLoginSuccess(request, response, authentication);

        logs.assertContains(Level.INFO, "Remember-me authentication successful");
    }
}