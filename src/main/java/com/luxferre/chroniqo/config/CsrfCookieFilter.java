package com.luxferre.chroniqo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Forces the deferred {@link CsrfToken} to be resolved on every request so
 * that the {@code XSRF-TOKEN} cookie is always present for the single-page
 * application. Without this filter the cookie would only be written when the
 * token is accessed by the application.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Rendering the token value triggers the cookie to be written.
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
