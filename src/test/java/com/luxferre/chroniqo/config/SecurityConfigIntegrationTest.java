package com.luxferre.chroniqo.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "/login",
            "/register",
            "/verify-email?token=test-token",
            "/request-password-reset",
            "/reset-password?token=test-token",
            "/reset-password-confirm?token=test-token"
    })
    void unauthenticatedPublicSpaRoutes_areAccessible(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/favicon.ico",
            "/sw.js"
    })
    void unauthenticatedPublicAssets_doNotReturnUnauthorized(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/auth/verify-email?token=invalid-token",
            "/api/auth/reset-password"
    })
    void unauthenticatedPublicAuthEndpoints_areNotUnauthorized(String path) throws Exception {
        if (path.equals("/api/auth/reset-password")) {
            mockMvc.perform(post(path)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"token":"invalid-token","newPassword":"NewPassword123!"}
                                    """))
                    .andExpect(status().isBadRequest());
            return;
        }

        mockMvc.perform(get(path))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/auth/me",
            "/month/2026/8"
    })
    void protectedRoutes_stillRequireAuthentication(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized());
    }
}
