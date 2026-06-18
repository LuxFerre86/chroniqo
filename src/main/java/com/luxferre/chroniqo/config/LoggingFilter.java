package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to add the current user ID to MDC for logging purposes.
 * This ensures every log entry includes the active user context.
 */
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    private final UserService userService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            try {
                User user = userService.getCurrentUser();
                MDC.put("userId", user.getId());
            } catch (Exception e) {
                MDC.put("userId", "anonymous");
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
