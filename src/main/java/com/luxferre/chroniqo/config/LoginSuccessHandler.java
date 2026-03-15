package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.user.UserService;
import com.vaadin.flow.spring.security.VaadinSavedRequestAwareAuthenticationSuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

import java.io.IOException;

/**
 * Post-login success handler that records the current timestamp as the
 * user's last-login time immediately after a successful form-based login.
 *
 * <p>Extends Vaadin's
 * {@link VaadinSavedRequestAwareAuthenticationSuccessHandler}
 * so that Vaadin's saved-request redirect logic is preserved, and only the
 * last-login update is added on top.
 *
 * @author Luxferre86
 * @since 27.02.2026
 */
@RequiredArgsConstructor
public class LoginSuccessHandler extends VaadinSavedRequestAwareAuthenticationSuccessHandler {

    private final UserService userService;

    /**
     * Invoked after a successful form login. Delegates the Vaadin redirect
     * to the parent class, then records the last-login timestamp.
     *
     * @param request        the current HTTP request
     * @param response       the current HTTP response
     * @param authentication the established authentication
     * @throws java.io.IOException              if the redirect fails
     * @throws jakarta.servlet.ServletException if an error occurs
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        super.onAuthenticationSuccess(request, response, authentication);
        userService.updateLastLogin(authentication.getName());
    }
}