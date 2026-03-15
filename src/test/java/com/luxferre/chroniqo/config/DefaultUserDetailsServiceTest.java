package com.luxferre.chroniqo.config;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultUserDetailsServiceTest {

    @InjectMocks
    private DefaultUserDetailsService service;

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // loadUserByUsername
    // =========================================================================

    @Nested
    class LoadUserByUsername {

        @Test
        void knownActiveUser_returnsCorrectUserDetails() {
            User user = activeUser("hashed_pw");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            UserDetails details = service.loadUserByUsername("user@example.com");

            assertThat(details.getUsername()).isEqualTo("user@example.com");
            assertThat(details.getPassword()).isEqualTo("hashed_pw");
            assertThat(details.isEnabled()).isTrue();
            assertThat(details.isAccountNonLocked()).isTrue();
            assertThat(details.isAccountNonExpired()).isTrue();
            assertThat(details.isCredentialsNonExpired()).isTrue();
            assertThat(details.getAuthorities()).hasSize(1);
            assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        }

        @Test
        void unknownEmail_throwsUsernameNotFoundException() {
            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("nobody@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        void disabledUser_returnsDetailsWithEnabledFalse() {
            User user = activeUser("hash");
            user.setEnabled(false);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            UserDetails details = service.loadUserByUsername("user@example.com");

            assertThat(details.isEnabled()).isFalse();
        }

        @Test
        void lockedUser_returnsDetailsWithAccountNonLockedFalse() {
            User user = activeUser("hash");
            user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            UserDetails details = service.loadUserByUsername("user@example.com");

            assertThat(details.isAccountNonLocked()).isFalse();
        }

        @Test
        void expiredLock_returnsDetailsWithAccountNonLockedTrue() {
            User user = activeUser("hash");
            user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            UserDetails details = service.loadUserByUsername("user@example.com");

            assertThat(details.isAccountNonLocked()).isTrue();
        }
    }

    // =========================================================================
    // getUsernameFromContext
    // =========================================================================

    @Nested
    class GetUsernameFromContext {

        @Test
        void authenticatedContext_returnsUsername() {
            org.springframework.security.core.userdetails.User principal =
                    new org.springframework.security.core.userdetails.User(
                            "user@example.com", "hash", java.util.Collections.emptyList());

            SecurityContext ctx = new SecurityContextImpl();
            ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                    principal, null, java.util.Collections.emptyList()));
            SecurityContextHolder.setContext(ctx);

            assertThat(service.getUsernameFromContext()).contains("user@example.com");
        }

        @Test
        void emptyContext_returnsEmpty() {
            SecurityContextHolder.clearContext();
            assertThat(service.getUsernameFromContext()).isEmpty();
        }

        @Test
        void contextWithNonUserPrincipal_returnsEmpty() {
            SecurityContext ctx = new SecurityContextImpl();
            ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                    "raw-string-principal", null));
            SecurityContextHolder.setContext(ctx);

            assertThat(service.getUsernameFromContext()).isEmpty();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private User activeUser(String passwordHash) {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash(passwordHash);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        return user;
    }
}