package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.frontend.LoginView;
import com.luxferre.chroniqo.service.AuthenticationService;
import com.vaadin.flow.spring.security.VaadinSavedRequestAwareAuthenticationSuccessHandler;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {"/public/**",
            "/login",
            "/register",
            "/reset-password",
            "/verify-email",
            "/images/**",
            "/styles/**",
            "/icons/**",
            "/*.css",
            "/dark"
    };

    private final RememberMeProperties rememberMeProperties;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, VaadinSavedRequestAwareAuthenticationSuccessHandler authenticationSuccessHandler) {
        // register custom authenticationSuccessHandler as shared object
        http.setSharedObject(VaadinSavedRequestAwareAuthenticationSuccessHandler.class, authenticationSuccessHandler);
        // Configure your static resources with public access
        http.authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_PATHS)
                .permitAll());
        // Vaadin Security
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class));
        // Remember Me Configuration
        http.rememberMe(remember -> remember
                .key(rememberMeProperties.getKey())
                .tokenValiditySeconds((int) rememberMeProperties.getValidity().toSeconds()) // 30 days
                .useSecureCookie(rememberMeProperties.isUseSecureCookie())
                .rememberMeCookieDomain(rememberMeProperties.getCookieDomain())
                .rememberMeParameter("remember-me")
        );

        return http.build();
    }

    @Bean
    public VaadinSavedRequestAwareAuthenticationSuccessHandler loginSuccessHandler(AuthenticationService authenticationService) {
        return new LoginSuccessHandler(authenticationService);
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
