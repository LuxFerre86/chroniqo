package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser("user@example.com")
@DisplayName("MeController")
class MeControllerTest extends AbstractApiControllerTest {

    private static final String PROFILE_BODY = """
            {"firstName":"  Ada ","lastName":"Lovelace","weeklyTargetHours":32,
             "workingDays":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY"],
             "countryCode":"de","subdivisionCode":"de-by"}
            """;

    private User user() {
        User user = new User();
        user.setId("user-id");
        user.setEmail("user@example.com");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setWeeklyTargetHours(new BigDecimal("32.0"));
        user.setWorkingDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY));
        user.setCountryCode("DE");
        user.setSubdivisionCode("DE-BY");
        user.setCreatedAt(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        return user;
    }

    @Test
    void me_returnsProfileWithoutSensitiveFields() throws Exception {
        User user = user();
        user.setPasswordHash("secret-hash");
        when(userService.getCurrentUser()).thenReturn(user);

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.weeklyTargetHours").value(32.0))
                .andExpect(jsonPath("$.workingDays[0]").value("MONDAY"))
                .andExpect(jsonPath("$.workingDays[1]").value("TUESDAY"))
                .andExpect(jsonPath("$.countryCode").value("DE"))
                .andExpect(jsonPath("$.subdivisionCode").value("DE-BY"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    void me_userVanished_returns401() throws Exception {
        when(userService.getCurrentUser())
                .thenThrow(new com.luxferre.chroniqo.service.user.UserNotFoundException("gone"));

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void updateProfile_normalizesInputDelegatesToServiceAndReturnsProfile() throws Exception {
        when(userService.getCurrentUser()).thenReturn(user());

        mockMvc.perform(put("/api/me").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));

        verify(userService).updateProfile(
                "user@example.com", "Ada", "Lovelace", BigDecimal.valueOf(32),
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
                "DE", "DE-BY");
    }

    @Test
    void updateProfile_withoutCountry_passesNulls() throws Exception {
        when(userService.getCurrentUser()).thenReturn(user());
        String body = """
                {"firstName":"Ada","lastName":"Lovelace","weeklyTargetHours":40,
                 "workingDays":["MONDAY"],"countryCode":"","subdivisionCode":"DE-BY"}
                """;

        mockMvc.perform(put("/api/me").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(userService).updateProfile(eq("user@example.com"), eq("Ada"), eq("Lovelace"), eq(BigDecimal.valueOf(40)),
                eq(Set.of(DayOfWeek.MONDAY)), isNull(), isNull());
    }

    @Test
    void updateProfile_decimalWeeklyTargetHours_isAcceptedAndPassedAsBigDecimal() throws Exception {
        when(userService.getCurrentUser()).thenReturn(user());
        String body = """
                {"firstName":"Ada","lastName":"Lovelace","weeklyTargetHours":37.5,
                 "workingDays":["MONDAY"],"countryCode":"DE","subdivisionCode":"DE-BY"}
                """;

        mockMvc.perform(put("/api/me").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(userService).updateProfile(eq("user@example.com"), eq("Ada"), eq("Lovelace"),
                argThat((BigDecimal h) -> h != null && h.compareTo(new BigDecimal("37.5")) == 0),
                eq(Set.of(DayOfWeek.MONDAY)), eq("DE"), eq("DE-BY"));
    }

    @Test
    void updateProfile_tooManyDecimals_returns400() throws Exception {
        String body = """
                {"firstName":"Ada","lastName":"Lovelace","weeklyTargetHours":37.55,
                 "workingDays":["MONDAY"]}
                """;

        mockMvc.perform(put("/api/me").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.weeklyTargetHours").exists());

        verify(userService, never()).updateProfile(any(), any(), any(), any(), anySet(), any(), any());
    }

    @Test
    void me_decimalWeeklyTargetHours_isSerializedAsJsonNumber() throws Exception {
        User user = user();
        user.setWeeklyTargetHours(new BigDecimal("37.5"));
        when(userService.getCurrentUser()).thenReturn(user);

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyTargetHours").value(37.5));
    }

    @Test
    void updateProfile_invalidBody_returns400WithErrorsMap() throws Exception {
        String body = """
                {"firstName":"","lastName":"Lovelace","weeklyTargetHours":99,"workingDays":[]}
                """;

        mockMvc.perform(put("/api/me").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.weeklyTargetHours").exists())
                .andExpect(jsonPath("$.errors.workingDays").exists());

        verify(userService, never()).updateProfile(any(), any(), any(), any(), anySet(), any(), any());
    }

    @Test
    void updateProfile_unknownCountry_returns400() throws Exception {
        when(userService.getCurrentUser()).thenReturn(user());
        String body = """
                {"firstName":"Ada","lastName":"Lovelace","weeklyTargetHours":40,
                 "workingDays":["MONDAY"],"countryCode":"ZZ"}
                """;

        mockMvc.perform(put("/api/me").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Unknown country: ZZ"));

        verify(userService, never()).updateProfile(any(), any(), any(), any(), anySet(), any(), any());
    }

    @Test
    void updateProfile_subdivisionOfOtherCountry_returns400() throws Exception {
        when(userService.getCurrentUser()).thenReturn(user());
        String body = """
                {"firstName":"Ada","lastName":"Lovelace","weeklyTargetHours":40,
                 "workingDays":["MONDAY"],"countryCode":"DE","subdivisionCode":"FR-IDF"}
                """;

        mockMvc.perform(put("/api/me").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void changePassword_returns204AndDelegatesToService() throws Exception {
        when(userService.getCurrentUser()).thenReturn(user());

        mockMvc.perform(put("/api/me/password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-password\",\"newPassword\":\"Secur3P@ssword\"}"))
                .andExpect(status().isNoContent());

        verify(userService).changePassword("user@example.com", "old-password", "Secur3P@ssword");
    }

    @Test
    void changePassword_weakNewPassword_returns400() throws Exception {
        mockMvc.perform(put("/api/me/password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-password\",\"newPassword\":\"weak\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.newPassword").value("Password must be at least 12 characters"));

        verify(userService, never()).changePassword(any(), any(), any());
    }

    @Test
    void changePassword_wrongCurrentPassword_returns400() throws Exception {
        when(userService.getCurrentUser()).thenReturn(user());
        doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(userService).changePassword(any(), any(), any());

        mockMvc.perform(put("/api/me/password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"Secur3P@ssword\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void deleteAccount_returns204AndDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/me").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"my-password\"}"))
                .andExpect(status().isNoContent());

        verify(userService).deleteCurrentUserAccount("my-password");
    }

    @Test
    void deleteAccount_blankPassword_returns400() throws Exception {
        mockMvc.perform(delete("/api/me").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verify(userService, never()).deleteCurrentUserAccount(any());
    }

    @Test
    void deleteAccount_withoutCsrfToken_returns403() throws Exception {
        mockMvc.perform(delete("/api/me").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"my-password\"}"))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteCurrentUserAccount(any());
    }
}
