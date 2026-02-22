package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.frontend.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {"/public/**", "/login", "/register", "/reset-password", "/verify-email", "/images/**", "/styles/**", "/icons/**","/*.css","/dark"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // Configure your static resources with public access
        http.authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_PATHS)
                .permitAll());

        // Vaadin Security
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.loginView(LoginView.class);
            //configurer.defaultSuccessUrl("/month");
        });
        // Remember Me Configuration
        http.rememberMe(remember -> remember
                .key("chroniqo-remember-me-key")
                .tokenValiditySeconds(30 * 24 * 60 * 60) // 30 days
                .rememberMeParameter("remember-me")
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
