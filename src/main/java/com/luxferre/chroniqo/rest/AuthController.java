package com.luxferre.chroniqo.rest;

import com.luxferre.chroniqo.dto.UserRegistrationRequest;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.user.EmailVerificationResult;
import com.luxferre.chroniqo.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for authentication and account management endpoints
 * that do not require a pre-existing session.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(UserProfileResponse.of(user));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody UserRegistrationRequest request) {
        userService.register(request);
        return ResponseEntity.ok(Map.of("message", "Registration successful. Please check your email to verify your account."));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        EmailVerificationResult result = userService.verifyEmail(token);
        return switch (result) {
            case VERIFIED_LOGGED_IN, VERIFIED_LOGIN_REQUIRED ->
                    ResponseEntity.ok(Map.of("result", result.name()));
            case INVALID ->
                    ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired verification token."));
        };
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody Map<String, String> body) {
        userService.requestPasswordReset(body.get("email"));
        return ResponseEntity.ok(Map.of("message", "If the email address is registered, you will receive a reset link shortly."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        boolean success = userService.resetPassword(body.get("token"), body.get("newPassword"));
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired reset token."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out."));
    }

    public record UserProfileResponse(
            String email,
            String firstName,
            String lastName,
            int weeklyTargetHours,
            Set<DayOfWeek> workingDays,
            String countryCode,
            String subdivisionCode
    ) {
        static UserProfileResponse of(User user) {
            return new UserProfileResponse(
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getWeeklyTargetHours(),
                    user.getWorkingDaysOrDefault(),
                    user.getCountryCode(),
                    user.getSubdivisionCode()
            );
        }
    }
}
