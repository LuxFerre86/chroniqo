package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

/**
 * Spring Security {@link UserDetailsService}
 * implementation that loads user data from the database by email address.
 *
 * <p>All users are granted a single authority ({@code ROLE_USER}). Account
 * locking is time-based: a non-null {@code User.lockedUntil} that lies in the
 * future causes {@link UserDetails#isAccountNonLocked()}
 * to return {@code false}. Once the timestamp lapses the lock clears
 * automatically — no manual intervention or scheduler is needed.
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
@Service
@RequiredArgsConstructor
public class DefaultUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a {@link UserDetails}
     * object for the given email address.
     *
     * <p>The returned object reflects the current enabled/locked state of the
     * account. A generic {@code "Bad credentials"} message is used for the
     * {@link UsernameNotFoundException}
     * to avoid leaking whether an email exists.
     *
     * @param email the user's email address (used as the username)
     * @return populated {@link UserDetails}
     * @throws UsernameNotFoundException if no user with the given email exists
     */
    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .accountExpired(false)
                .accountLocked(user.isAccountLocked())
                .credentialsExpired(false)
                .disabled(!user.isEnabled())
                .build();
    }

    /**
     * Extracts the username (email) of the currently authenticated principal
     * from the {@link SecurityContextHolder}.
     *
     * @return an {@link java.util.Optional} containing the email, or empty if
     * there is no authentication or the principal is not a
     * {@link org.springframework.security.core.userdetails.User} instance
     */
    public Optional<String> getUsernameFromContext() {
        SecurityContext context = SecurityContextHolder.getContext();
        Object principal = Optional.of(context).map(SecurityContext::getAuthentication).map(Authentication::getPrincipal).orElse(null);
        return Optional.ofNullable(principal).filter(org.springframework.security.core.userdetails.User.class::isInstance).map(org.springframework.security.core.userdetails.User.class::cast).map(org.springframework.security.core.userdetails.User::getUsername);
    }
}