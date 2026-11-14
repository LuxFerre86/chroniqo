package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.api.dto.PasswordResetConfirmRequest;
import com.luxferre.chroniqo.api.dto.PasswordResetRequestRequest;
import com.luxferre.chroniqo.api.dto.RegisterRequest;
import com.luxferre.chroniqo.api.dto.VerifyEmailRequest;
import com.luxferre.chroniqo.api.dto.VerifyEmailResponse;
import com.luxferre.chroniqo.dto.UserRegistrationRequest;
import com.luxferre.chroniqo.service.user.EmailAlreadyRegisteredException;
import com.luxferre.chroniqo.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public account lifecycle endpoints: registration, email verification and
 * password reset. Login and logout are handled by Spring Security
 * ({@code POST /api/auth/login}, {@code POST /api/auth/logout}).
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ProfileValidator profileValidator;

    /**
     * Registers a new, not yet verified account and sends the verification email.
     * An already registered email address is answered exactly like a successful
     * registration so that callers cannot find out which addresses exist.
     *
     * @param request the registration data
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        String countryCode = profileValidator.normalizeCountry(request.countryCode());
        String subdivisionCode = profileValidator.normalizeSubdivision(countryCode, request.subdivisionCode());
        try {
            userService.register(new UserRegistrationRequest(
                    request.email().trim(),
                    request.password(),
                    request.firstName().trim(),
                    request.lastName().trim(),
                    request.weeklyTargetHours(),
                    countryCode,
                    subdivisionCode));
        } catch (EmailAlreadyRegisteredException ignored) {
            // Answer exactly like a successful registration so registered emails cannot be enumerated.
        }
    }

    /**
     * Verifies an email address using the token from the verification link. On
     * success the user is logged in automatically if possible.
     *
     * @param request the verification token
     * @return the verification outcome
     */
    @PostMapping("/verify-email")
    public VerifyEmailResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return new VerifyEmailResponse(userService.verifyEmail(request.token()));
    }

    /**
     * Requests a password-reset email. Always answers {@code 202} so that
     * callers cannot find out which email addresses are registered.
     *
     * @param request the account email
     */
    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequestRequest request) {
        userService.requestPasswordReset(request.email().trim());
    }

    /**
     * Sets a new password using the token from the reset email.
     *
     * @param request token and new password
     * @return {@code 204} on success, {@code 400} if the token is invalid or expired
     */
    @PostMapping("/password-reset/confirm")
    public org.springframework.http.ResponseEntity<Object> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        if (userService.resetPassword(request.token(), request.newPassword())) {
            return org.springframework.http.ResponseEntity.noContent().build();
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The reset link is invalid or has expired.");
        problem.setProperty("code", "INVALID_TOKEN");
        return org.springframework.http.ResponseEntity.badRequest().body(problem);
    }
}
