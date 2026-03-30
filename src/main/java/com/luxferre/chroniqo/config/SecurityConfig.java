package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.frontend.LoginView;
import com.luxferre.chroniqo.service.user.UserService;
import com.vaadin.flow.spring.security.VaadinSavedRequestAwareAuthenticationSuccessHandler;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.util.StringUtils;

/**
 * Central Spring Security configuration for Chroniqo.
 *
 * <p>Defines the {@link SecurityFilterChain},
 * remember-me services, login success handler, password encoder, and
 * authentication event publisher. Public paths (login, registration, email
 * verification, static resources) are explicitly permitted; all other routes
 * require an authenticated session. Vaadin-specific security integration is
 * applied via {@link VaadinSecurityConfigurer}.
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
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
            "/dark",
            "/actuator/**"
    };

    private final RememberMeProperties rememberMeProperties;

    /**
     * Configures the main security filter chain.
     *
     * <p>Public paths are permitted without authentication; all other requests
     * are handled by Vaadin's security integration. Remember-me services and
     * the custom login success handler are wired in here.
     *
     * @param http                         the security builder
     * @param authenticationSuccessHandler the Vaadin-aware success handler
     * @param rememberMeServices           the remember-me services bean
     * @return the configured {@link org.springframework.security.web.SecurityFilterChain}
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, VaadinSavedRequestAwareAuthenticationSuccessHandler authenticationSuccessHandler, LastLoginTokenBasedRememberMeServices rememberMeServices) {
        // register custom authenticationSuccessHandler as shared object
        http.setSharedObject(VaadinSavedRequestAwareAuthenticationSuccessHandler.class, authenticationSuccessHandler);
        // Configure your static resources with public access
        http.authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_PATHS)
                .permitAll());
        // Vaadin Security
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> configurer.loginView(LoginView.class));
        // Remember Me Configuration
        http.rememberMe(remember -> remember.rememberMeServices(rememberMeServices));

        return http.build();
    }

    /**
     * Creates the remember-me services bean configured from
     * {@link RememberMeProperties}. The cookie domain is only set when the
     * property is non-blank, so local development works without the variable.
     *
     * @param userDetailsService the user-details service for token validation
     * @param userService        used to record last-login on cookie renewal
     * @return configured {@link LastLoginTokenBasedRememberMeServices}
     */
    @Bean
    public LastLoginTokenBasedRememberMeServices lastLoginTokenBasedRememberMeServices(UserDetailsService userDetailsService, UserService userService) {
        LastLoginTokenBasedRememberMeServices rememberMeServices = new LastLoginTokenBasedRememberMeServices(rememberMeProperties.getKey(), userDetailsService, userService, TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256);
        rememberMeServices.setUseSecureCookie(rememberMeProperties.isUseSecureCookie());
        if (StringUtils.hasText(rememberMeProperties.getCookieDomain())) {
            rememberMeServices.setCookieDomain(rememberMeProperties.getCookieDomain());
        }
        rememberMeServices.setTokenValiditySeconds(Math.toIntExact(rememberMeProperties.getValidity().getSeconds()));
        return rememberMeServices;
    }

    /**
     * Creates the login success handler that records the last-login timestamp
     * after each successful form login.
     *
     * @param userService used to persist the last-login timestamp
     * @return a {@link LoginSuccessHandler} instance
     */
    @Bean
    public VaadinSavedRequestAwareAuthenticationSuccessHandler loginSuccessHandler(UserService userService) {
        return new LoginSuccessHandler(userService);
    }


    /**
     * Password encoder using BCrypt with a work factor of 12.
     *
     * @return a {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Enables publishing of Spring Security authentication events
     * (success and failure), which are required for brute-force protection
     * via {@link AuthenticationEventListener}.
     */
    @Bean
    public DefaultAuthenticationEventPublisher authenticationEventPublisher() {
        return new DefaultAuthenticationEventPublisher();
    }
}