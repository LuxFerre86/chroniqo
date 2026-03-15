package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

/**
 * Extension of {@link TokenBasedRememberMeServices}
 * that records the last-login timestamp in the database whenever a remember-me
 * cookie is successfully used to authenticate a user.
 *
 * <p>The timestamp update is delegated to
 * {@link UserService#updateLastLogin(String)}
 * after the standard remember-me token processing in the parent class.
 *
 * @author Luxferre86
 * @since 28.02.2026
 */
public class LastLoginTokenBasedRememberMeServices extends TokenBasedRememberMeServices {

    private final UserService userService;

    public LastLoginTokenBasedRememberMeServices(String key, UserDetailsService userDetailsService, UserService userService, RememberMeTokenAlgorithm encodingAlgorithm) {
        super(key, userDetailsService, encodingAlgorithm);
        this.userService = userService;
    }

    /**
     * Invoked by the parent class after a remember-me token has been
     * successfully processed. Records the current timestamp as the
     * user's last-login time in addition to issuing the cookie.
     *
     * @param request                  the current HTTP request
     * @param response                 the current HTTP response
     * @param successfulAuthentication the authentication that was established
     */
    @Override
    public void onLoginSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Authentication successfulAuthentication) {
        super.onLoginSuccess(request, response, successfulAuthentication);
        userService.updateLastLogin(successfulAuthentication.getName());
    }
}