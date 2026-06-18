package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.user.UserService;
import com.luxferre.chroniqo.util.LoggingTestUtils;
import ch.qos.logback.classic.Level;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginSuccessHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    private LoginSuccessHandler handler;
    private LoggingTestUtils logs;

    @BeforeEach
    void setUp() {
        handler = new LoginSuccessHandler(userService);
        logs = LoggingTestUtils.captureLogsFor(LoginSuccessHandler.class);
    }

    @AfterEach
    void tearDown() {
        logs.stop();
    }

    @Test
    void onAuthenticationSuccess_delegatesUpdateLastLoginToUserService() throws IOException, ServletException {
        when(authentication.getName()).thenReturn("user@example.com");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userService).updateLastLogin("user@example.com");
    }

    @Test
    void onAuthenticationSuccess_usesAuthenticationNameAsEmailIdentifier() throws IOException, ServletException {
        when(authentication.getName()).thenReturn("other@example.com");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userService).updateLastLogin("other@example.com");
        verify(userService, never()).updateLastLogin("user@example.com");
    }

    @Test
    void onAuthenticationSuccess_logsSuccessfulAuthentication() throws IOException, ServletException {
        when(authentication.getName()).thenReturn("test@example.com");

        handler.onAuthenticationSuccess(request, response, authentication);

        logs.assertContains(Level.INFO, "User login successful");
    }
}

