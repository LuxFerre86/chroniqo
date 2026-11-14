package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.util.StringUtils;

/**
 * Central Spring Security configuration for Chroniqo.
 *
 * <p>Defines the {@link SecurityFilterChain}, remember-me services, login
 * success/failure handling, password encoder, and authentication event
 * publisher. The REST API under {@code /api} requires an authenticated
 * session, except for the explicitly public endpoints (login, registration,
 * email verification, password reset, public configuration and country
 * lists). Everything else (the bundled single-page application and its static
 * assets) is publicly served.
 *
 * <p>Authentication is session-based with an optional remember-me cookie.
 * State-changing requests are protected with a cookie-based CSRF token
 * ({@code XSRF-TOKEN} cookie, {@code X-XSRF-TOKEN} header).
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    static final String LOGIN_URL = "/api/auth/login";
    static final String LOGOUT_URL = "/api/auth/logout";

    private static final String[] PUBLIC_API_PATHS = {
            "/api/config",
            "/api/countries/**",
            "/api/auth/**",
            "/actuator/health/**"
    };

    private static final String LOGIN_FAILED_BODY =
            "{\"status\":401,\"title\":\"Unauthorized\",\"code\":\"LOGIN_FAILED\","
                    + "\"detail\":\"Invalid email or password, or the account is locked or not verified.\"}";

    private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; "
            + "img-src 'self' data:; "
            + "style-src 'self' 'unsafe-inline'; "
            + "font-src 'self' data:; "
            + "connect-src 'self'; "
            + "manifest-src 'self'; "
            + "worker-src 'self'; "
            + "object-src 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'; "
            + "frame-ancestors 'none'";

    private final RememberMeProperties rememberMeProperties;

    /**
     * Configures the main security filter chain.
     *
     * @param http                         the security builder
     * @param authenticationSuccessHandler the handler recording the last login
     * @param rememberMeServices           the remember-me services bean
     * @param loggingFilter                the logging filter
     * @return the configured {@link org.springframework.security.web.SecurityFilterChain}
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, LoginSuccessHandler authenticationSuccessHandler, LastLoginTokenBasedRememberMeServices rememberMeServices, LoggingFilter loggingFilter) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_API_PATHS).permitAll()
                .requestMatchers("/api/**").authenticated()
                // SPA shell, static assets and client-side routes
                .anyRequest().permitAll());

        http.csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()));
        http.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);

        http.formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl(LOGIN_URL)
                .successHandler(authenticationSuccessHandler)
                .failureHandler(loginFailureHandler())
                .permitAll());

        http.logout(logout -> logout
                .logoutUrl(LOGOUT_URL)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                .deleteCookies("JSESSIONID")
                .permitAll());

        http.rememberMe(remember -> remember.rememberMeServices(rememberMeServices));

        http.exceptionHandling(handling -> handling
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY)));

        // Add logging filter to include user in MDC
        http.addFilterBefore(loggingFilter, SecurityContextHolderFilter.class);

        return http.build();
    }

    /**
     * Cookie-based CSRF token repository. The cookie must stay readable for the
     * single-page application, so it is not {@code HttpOnly}; {@code SameSite=Lax}
     * keeps it from being sent on cross-site subrequests.
     *
     * @return the CSRF token repository
     */
    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));
        return repository;
    }

    /**
     * Failure handler answering failed logins with a JSON {@code 401}. The same
     * response is used for every failure reason so that no information about
     * existing accounts is leaked.
     *
     * @return the authentication failure handler
     */
    private AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/problem+json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(LOGIN_FAILED_BODY);
        };
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
    public LoginSuccessHandler loginSuccessHandler(UserService userService) {
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

    /**
     * Creates the logging filter bean to add user context to MDC.
     *
     * @param userService the user service for retrieving current user
     * @return the configured LoggingFilter
     */
    @Bean
    public LoggingFilter loggingFilter(UserService userService) {
        return new LoggingFilter(userService);
    }
}
