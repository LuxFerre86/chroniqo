package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

public class LastLoginTokenBasedRememberMeServices extends TokenBasedRememberMeServices {

    private final UserService userService;

    public LastLoginTokenBasedRememberMeServices(String key, UserDetailsService userDetailsService, UserService userService, RememberMeTokenAlgorithm encodingAlgorithm) {
        super(key, userDetailsService, encodingAlgorithm);
        this.userService = userService;
    }

    @Override
    public void onLoginSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Authentication successfulAuthentication) {
        super.onLoginSuccess(request, response, successfulAuthentication);
        userService.updateLastLogin(successfulAuthentication.getName());
    }
}
