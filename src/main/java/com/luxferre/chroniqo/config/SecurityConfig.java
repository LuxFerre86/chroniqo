package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Central Spring Security configuration for Chroniqo.
 *
 * <p>Configures session-based form login that returns JSON responses instead
 * of HTML redirects, making it compatible with the Vue SPA frontend. CSRF
 * protection is handled via the {@code XSRF-TOKEN} cookie so that the Vue
 * Axios client can read and forward the token automatically.
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/api/public/**",
            "/api/auth/register",
            "/api/auth/verify-email",
            "/api/auth/request-password-reset",
            "/api/auth/reset-password",
            "/login",
            "/register",
            "/verify-email",
            "/request-password-reset",
            "/reset-password",
            "/reset-password-confirm",
            "/assets/**",
            "/icons/**",
            "/sw.js",
            "/favicon.ico",
            "/favicon.png",
            "/index.html",
            "/",
            "/actuator/health/**"
    };

    private final RememberMeProperties rememberMeProperties;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            LoginSuccessHandler successHandler,
                                            LastLoginTokenBasedRememberMeServices rememberMeServices,
                                            LoggingFilter loggingFilter) throws Exception {
        // CSRF – cookie-based so the Vue SPA can read and submit the token
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        http.csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfHandler)
                .ignoringRequestMatchers("/api/auth/login", "/api/auth/logout")
        );

        // Public paths
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated()
        );

        // Form login – returns JSON, not a redirect
        http.formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(jsonFailureHandler())
                .permitAll()
        );

        // Logout – returns JSON
        http.logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> {
                    res.setStatus(200);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"message\":\"Logged out.\"}");
                })
        );

        // Remember-me
        http.rememberMe(remember -> remember.rememberMeServices(rememberMeServices));

        // Session management – return 401 JSON on unauthenticated access
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authException) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
        );

        // Logging filter
        http.addFilterBefore(loggingFilter, SecurityContextHolderFilter.class);
        // Ensure the CSRF token cookie is created on first request
        http.addFilterAfter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (csrfToken != null) {
                    csrfToken.getToken();
                }
                filterChain.doFilter(request, response);
            }
        }, CsrfFilter.class);

        return http.build();
    }

    private AuthenticationFailureHandler jsonFailureHandler() {
        return (request, response, exception) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid credentials\"}");
        };
    }

    @Bean
    public LastLoginTokenBasedRememberMeServices lastLoginTokenBasedRememberMeServices(
            UserDetailsService userDetailsService, UserService userService) {
        LastLoginTokenBasedRememberMeServices rememberMeServices =
                new LastLoginTokenBasedRememberMeServices(
                        rememberMeProperties.getKey(), userDetailsService, userService,
                        TokenBasedRememberMeServices.RememberMeTokenAlgorithm.SHA256);
        rememberMeServices.setUseSecureCookie(rememberMeProperties.isUseSecureCookie());
        if (StringUtils.hasText(rememberMeProperties.getCookieDomain())) {
            rememberMeServices.setCookieDomain(rememberMeProperties.getCookieDomain());
        }
        rememberMeServices.setTokenValiditySeconds(
                Math.toIntExact(rememberMeProperties.getValidity().getSeconds()));
        return rememberMeServices;
    }

    @Bean
    public LoginSuccessHandler loginSuccessHandler(UserService userService) {
        return new LoginSuccessHandler(userService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DefaultAuthenticationEventPublisher authenticationEventPublisher() {
        return new DefaultAuthenticationEventPublisher();
    }

    @Bean
    public LoggingFilter loggingFilter(UserService userService) {
        return new LoggingFilter(userService);
    }
}
