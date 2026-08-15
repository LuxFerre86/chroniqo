package com.luxferre.chroniqo.rest;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for authenticated user profile and account management.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    public ResponseEntity<Map<String, String>> updateProfile(@RequestBody UpdateProfileRequest request) {
        User user = userService.getCurrentUser();
        userService.updateProfile(
                user.getEmail(),
                request.firstName(),
                request.lastName(),
                request.weeklyTargetHours(),
                request.workingDays(),
                request.countryCode(),
                request.subdivisionCode()
        );
        return ResponseEntity.ok(Map.of("message", "Profile updated."));
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePasswordRequest request) {
        User user = userService.getCurrentUser();
        userService.changePassword(user.getEmail(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed."));
    }

    @DeleteMapping("/account")
    public ResponseEntity<Map<String, String>> deleteAccount(@RequestBody Map<String, String> body) {
        userService.deleteCurrentUserAccount(body.get("password"));
        return ResponseEntity.ok(Map.of("message", "Account deleted."));
    }

    public record UpdateProfileRequest(
            String firstName,
            String lastName,
            int weeklyTargetHours,
            Set<DayOfWeek> workingDays,
            String countryCode,
            String subdivisionCode
    ) {}

    public record ChangePasswordRequest(String oldPassword, String newPassword) {}
}
