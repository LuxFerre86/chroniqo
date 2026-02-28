package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

public class LastLoginTokenBasedRememberMeServices extends TokenBasedRememberMeServices {

    private final AuthenticationService authenticationService;

    public LastLoginTokenBasedRememberMeServices(String key, UserDetailsService userDetailsService, AuthenticationService authenticationService, RememberMeTokenAlgorithm encodingAlgorithm) {
        super(key, userDetailsService, encodingAlgorithm);
        this.authenticationService = authenticationService;
    }

    @Override
    public void onLoginSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Authentication successfulAuthentication) {
        super.onLoginSuccess(request, response, successfulAuthentication);
        authenticationService.updateLastLogin(successfulAuthentication.getName());
    }
}
