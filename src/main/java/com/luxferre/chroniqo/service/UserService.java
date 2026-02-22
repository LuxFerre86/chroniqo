package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.config.DefaultUserDetailsService;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DefaultUserDetailsService userDetailsService;

    public User getCurrentUser() {
        return userRepository.findByEmail(userDetailsService.getUsernameFromContext()).orElseThrow(() -> new IllegalArgumentException("Could not determine current user."));
    }
}
