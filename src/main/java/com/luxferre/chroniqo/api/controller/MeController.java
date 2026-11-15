package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.api.dto.ChangePasswordRequest;
import com.luxferre.chroniqo.api.dto.DeleteAccountRequest;
import com.luxferre.chroniqo.api.dto.UpdateProfileRequest;
import com.luxferre.chroniqo.api.dto.UserProfileResponse;
import com.luxferre.chroniqo.config.LastLoginTokenBasedRememberMeServices;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the authenticated user's own profile and account.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final ProfileValidator profileValidator;
    private final LastLoginTokenBasedRememberMeServices rememberMeServices;

    /**
     * Returns the profile of the authenticated user.
     *
     * @return the user profile
     */
    @GetMapping
    public UserProfileResponse me() {
        return UserProfileResponse.of(userService.getCurrentUser());
    }

    /**
     * Updates the profile of the authenticated user.
     *
     * @param request the new profile data
     * @return the updated profile
     */
    @PutMapping
    public UserProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.getCurrentUser();
        String countryCode = profileValidator.normalizeCountry(request.countryCode());
        String subdivisionCode = profileValidator.normalizeSubdivision(countryCode, request.subdivisionCode());
        userService.updateProfile(
                user.getEmail(),
                request.firstName().trim(),
                request.lastName().trim(),
                request.weeklyTargetHours(),
                request.workingDays(),
                countryCode,
                subdivisionCode);
        if (request.language() != null) {
            userService.updateLanguage(user.getEmail(), request.language());
        }
        return UserProfileResponse.of(userService.getCurrentUser());
    }

    /**
     * Changes the password of the authenticated user.
     *
     * @param request current and new password
     */
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = userService.getCurrentUser();
        userService.changePassword(user.getEmail(), request.currentPassword(), request.newPassword());
    }

    /**
     * Permanently deletes the account of the authenticated user including all
     * time entries and absences, then terminates the session.
     *
     * @param request the current password as confirmation
     * @param httpRequest  the current request
     * @param httpResponse the current response
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@Valid @RequestBody DeleteAccountRequest request,
                              HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {
        userService.deleteCurrentUserAccount(request.password());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        rememberMeServices.logout(httpRequest, httpResponse, authentication);
        new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, authentication);
    }
}
