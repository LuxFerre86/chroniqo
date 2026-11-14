package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Post-login success handler that records the current timestamp as the
 * user's last-login time immediately after a successful form-based login.
 *
 * <p>The login is performed by the single-page application via
 * {@code POST /api/auth/login}; the handler therefore answers with
 * {@code 204 No Content} instead of redirecting, and the client fetches the
 * user profile afterwards.
 *
 * @author Luxferre86
 * @since 27.02.2026
 */
@RequiredArgsConstructor
@Slf4j
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    /**
     * Invoked after a successful form login. Records the last-login timestamp
     * and answers with {@code 204 No Content}.
     *
     * @param request        the current HTTP request
     * @param response       the current HTTP response
     * @param authentication the established authentication
     */
    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        userService.updateLastLogin(authentication.getName());
        response.setStatus(HttpStatus.NO_CONTENT.value());
        log.info("User login successful");
    }
}
