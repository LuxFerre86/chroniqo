package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.dto.UserRegistrationRequest;
import com.luxferre.chroniqo.service.RegistrationDisabledException;
import com.luxferre.chroniqo.service.user.EmailAlreadyRegisteredException;
import com.luxferre.chroniqo.service.user.EmailVerificationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController")
class AuthControllerTest extends AbstractApiControllerTest {

    private static final String REGISTER_BODY = """
            {"email":"new@example.com","password":"Secur3P@ssword","firstName":" New ","lastName":"User",
             "weeklyTargetHours":40,"countryCode":"de","subdivisionCode":"de-by"}
            """;

    // ---- register -----------------------------------------------------------

    @Test
    void register_returns201AndPassesNormalizedData() throws Exception {
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated());

        verify(userService).register(new UserRegistrationRequest(
                "new@example.com", "Secur3P@ssword",                 "New", "User", java.math.BigDecimal.valueOf(40), "DE", "DE-BY"));
    }

                    @Test
                    void register_withDecimalWeeklyTargetHours_isAccepted() throws Exception {
                        String body = """
                                {"email":"new@example.com","password":"Secur3P@ssword","firstName":"New","lastName":"User",
                                 "weeklyTargetHours":37.5}
                                """;

                        mockMvc.perform(post("/api/auth/register").with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isCreated());

                        verify(userService).register(new UserRegistrationRequest(
                                "new@example.com", "Secur3P@ssword", "New", "User", new java.math.BigDecimal("37.5"), null, null));
                    }

                    @Test
                    void register_withTooManyDecimals_returns400() throws Exception {
                        String body = """
                                {"email":"new@example.com","password":"Secur3P@ssword","firstName":"New","lastName":"User",
                                 "weeklyTargetHours":37.55}
                                """;

                        mockMvc.perform(post("/api/auth/register").with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON).content(body))
                                .andExpect(status().isBadRequest());

                        verify(userService, never()).register(any());
                    }

    @Test
    void register_withoutCountry_isAccepted() throws Exception {
        String body = """
                {"email":"new@example.com","password":"Secur3P@ssword","firstName":"New","lastName":"User",
                 "weeklyTargetHours":40}
                """;

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        verify(userService).register(new UserRegistrationRequest(
                "new@example.com", "Secur3P@ssword", "New", "User", java.math.BigDecimal.valueOf(40), null, null));
    }

    @Test
    void register_registrationDisabled_returns403() throws Exception {
        when(userService.register(any())).thenThrow(new RegistrationDisabledException("Registration is currently disabled."));

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REGISTRATION_DISABLED"));
    }

    @Test
    void register_emailAlreadyRegistered_isAnsweredLikeSuccess() throws Exception {
        when(userService.register(any())).thenThrow(new EmailAlreadyRegisteredException("Email already registered"));

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
    }

    @Test
    void register_invalidBody_returns400WithFieldErrors() throws Exception {
        String body = """
                {"email":"not-an-email","password":"weak","firstName":"","lastName":"User","weeklyTargetHours":120}
                """;

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.weeklyTargetHours").exists());

        verify(userService, never()).register(any());
    }

    @Test
    void register_unknownCountry_returns400() throws Exception {
        String body = """
                {"email":"new@example.com","password":"Secur3P@ssword","firstName":"New","lastName":"User",
                 "weeklyTargetHours":40,"countryCode":"ZZ"}
                """;

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verify(userService, never()).register(any());
    }

    @Test
    void register_withoutCsrfToken_returns403() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER_BODY))
                .andExpect(status().isForbidden());

        verify(userService, never()).register(any());
    }

    // ---- verify email -------------------------------------------------------

    @Test
    void verifyEmail_returnsStatusOfVerification() throws Exception {
        when(userService.verifyEmail("token-123")).thenReturn(EmailVerificationResult.VERIFIED_LOGIN_REQUIRED);

        mockMvc.perform(post("/api/auth/verify-email").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"token-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED_LOGIN_REQUIRED"));
    }

    @Test
    void verifyEmail_invalidToken_returnsInvalidStatus() throws Exception {
        when(userService.verifyEmail("bad")).thenReturn(EmailVerificationResult.INVALID);

        mockMvc.perform(post("/api/auth/verify-email").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"bad\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVALID"));
    }

    @Test
    void verifyEmail_blankToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.token").exists());
    }

    // ---- resend verification ------------------------------------------------

    @Test
    void resendVerificationEmail_alwaysReturns202() throws Exception {
        mockMvc.perform(post("/api/auth/verification-email/resend").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isAccepted());

        verify(userService).resendVerificationEmail("unknown@example.com");
    }

    @Test
    void resendVerificationEmail_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/verification-email/resend").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nope\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).resendVerificationEmail(any());
    }

    // ---- password reset -----------------------------------------------------

    @Test
    void requestPasswordReset_alwaysReturns202() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)                        .content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isAccepted());

        verify(userService).requestPasswordReset("unknown@example.com");
    }

    @Test
    void requestPasswordReset_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nope\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verify(userService, never()).requestPasswordReset(any());
    }

    @Test
    void confirmPasswordReset_validToken_returns204() throws Exception {
        when(userService.resetPassword("reset-token", "Secur3P@ssword")).thenReturn(true);

        mockMvc.perform(post("/api/auth/password-reset/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"reset-token\",\"newPassword\":\"Secur3P@ssword\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void confirmPasswordReset_invalidToken_returns400WithCode() throws Exception {
        when(userService.resetPassword("expired", "Secur3P@ssword")).thenReturn(false);

        mockMvc.perform(post("/api/auth/password-reset/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"expired\",\"newPassword\":\"Secur3P@ssword\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void confirmPasswordReset_weakPassword_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"reset-token\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.newPassword").exists());

        verify(userService, never()).resetPassword(any(), any());
    }

    @Test
    void unexpectedServiceFailure_isReportedAsInternalError() throws Exception {
        doThrow(new IllegalStateException("smtp down")).when(userService).requestPasswordReset(any());

        mockMvc.perform(post("/api/auth/password-reset/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@example.com\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }
}
