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

    public User getCurrentUser() throws UserNotFoundException {
        return userDetailsService.getUsernameFromContext().flatMap(userRepository::findByEmail).orElseThrow(() -> new UserNotFoundException("Could not determine current user."));
    }
}
