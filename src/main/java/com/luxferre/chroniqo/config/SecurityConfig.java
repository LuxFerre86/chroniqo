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

    @Bean
    public LastLoginTokenBasedRememberMeServices lastLoginTokenBasedRememberMeServices(UserDetailsService userDetailsService, UserService userService) {
        LastLoginTokenBasedRememberMeServices rememberMeServices = new LastLoginTokenBasedRememberMeServices(rememberMeProperties.getKey(), userDetailsService, userService, TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256);
        rememberMeServices.setUseSecureCookie(rememberMeProperties.isUseSecureCookie());
        rememberMeServices.setCookieDomain(rememberMeProperties.getCookieDomain());
        rememberMeServices.setTokenValiditySeconds(Math.toIntExact(rememberMeProperties.getValidity().getSeconds()));
        return rememberMeServices;
    }

    @Bean
    public VaadinSavedRequestAwareAuthenticationSuccessHandler loginSuccessHandler(UserService userService) {
        return new LoginSuccessHandler(userService);
    }


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
