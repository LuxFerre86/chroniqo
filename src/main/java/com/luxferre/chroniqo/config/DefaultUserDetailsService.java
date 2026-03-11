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

@Service
@RequiredArgsConstructor
public class DefaultUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

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

    public Optional<String> getUsernameFromContext() {
        SecurityContext context = SecurityContextHolder.getContext();
        Object principal = Optional.of(context).map(SecurityContext::getAuthentication).map(Authentication::getPrincipal).orElse(null);
        return Optional.ofNullable(principal).filter(org.springframework.security.core.userdetails.User.class::isInstance).map(org.springframework.security.core.userdetails.User.class::cast).map(org.springframework.security.core.userdetails.User::getUsername);
    }
}