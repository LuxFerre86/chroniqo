package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.user.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @BeforeEach
    void setUp() {
        handler = new LoginSuccessHandler(userService);
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
}