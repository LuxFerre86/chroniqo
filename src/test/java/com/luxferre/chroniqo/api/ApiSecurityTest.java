package com.luxferre.chroniqo.api;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real security filter chain (session login, CSRF, 401 handling,
 * public endpoints) against the H2 test database.
 */
@SpringBootTest
@DisplayName("API security")
class ApiSecurityTest {

    private static final String EMAIL = "api-security@example.com";
    private static final String PASSWORD = "Secur3P@ssword";

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void createUser() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setFirstName("Api");
        user.setLastName("Security");
        user.setWeeklyTargetHours(40);
        user.setEnabled(true);
        userRepository.save(user);
    }

    /**
     * Sends a matching {@code XSRF-TOKEN} cookie and {@code X-XSRF-TOKEN} header like the SPA does.
     * Deliberately not Spring Security's {@code csrf()} post-processor: that one permanently swaps the
     * token repository of the cached filter chain, which would hide the real cookie behaviour from the
     * other tests of this class.
     */
    private static RequestPostProcessor csrf() {
        String token = UUID.randomUUID().toString();
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", token));
            request.addHeader("X-XSRF-TOKEN", token);
            return request;
        };
    }

    @AfterEach
    void deleteUser() {
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    // ---- unauthenticated access ---------------------------------------------

    @Test
    void protectedEndpoint_withoutAuthentication_returns401ForJsonClients() throws Exception {
        mockMvc.perform(get("/api/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutAuthentication_returns401EvenForBrowsers() throws Exception {
        mockMvc.perform(get("/api/me").accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void allProtectedResources_requireAuthentication() throws Exception {
        for (String path : new String[]{"/api/time-entries?from=2026-03-01&to=2026-03-31",
                "/api/absences?from=2026-03-01&to=2026-03-31",
                "/api/summary?from=2026-03-01&to=2026-03-31", "/api/dashboard"}) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void unknownApiPath_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isUnauthorized());
    }

    // ---- public endpoints ---------------------------------------------------

    @Test
    void config_isPublic_andSetsCsrfCookieAndSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        hasItem(allOf(startsWith("XSRF-TOKEN="), not(containsStringIgnoringCase("HttpOnly"))))))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")));
    }

    private String fetchCsrfToken() throws Exception {
        return mockMvc.perform(get("/api/config")).andReturn().getResponse()
                .getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("XSRF-TOKEN="))
                .map(c -> c.substring("XSRF-TOKEN=".length(), c.indexOf(';')))
                .findFirst().orElseThrow();
    }

    @Test
    void countries_arePublic() throws Exception {
        mockMvc.perform(get("/api/countries")).andExpect(status().isOk());
        mockMvc.perform(get("/api/countries/DE/subdivisions")).andExpect(status().isOk());
    }

    // ---- CSRF ----------------------------------------------------------------

    @Test
    void stateChangingRequest_withoutCsrfToken_returns403() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void stateChangingRequest_withCookieAndHeaderToken_isAccepted() throws Exception {
        String token = fetchCsrfToken();

        mockMvc.perform(post("/api/auth/password-reset/request")
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void stateChangingRequest_withWrongHeaderToken_returns403() throws Exception {
        String token = fetchCsrfToken();

        mockMvc.perform(post("/api/auth/password-reset/request")
                        .cookie(new Cookie("XSRF-TOKEN", token))
                        .header("X-XSRF-TOKEN", "forged")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    // ---- login / logout ------------------------------------------------------

    @Test
    void login_withWrongPassword_returns401WithLoginFailedCode() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", EMAIL).param("password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(content().string(containsString("LOGIN_FAILED")))
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    void login_withUnknownUser_isIndistinguishableFromWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "nobody@example.com").param("password", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    void login_withUnverifiedAccount_returns401() throws Exception {
        User user = userRepository.findByEmail(EMAIL).orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", EMAIL).param("password", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    void login_withoutCsrfToken_returns403() throws Exception {
        mockMvc.perform(post("/api/auth/login").param("username", EMAIL).param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_success_returns204AndSessionGrantsAccess() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", EMAIL).param("password", PASSWORD))
                .andExpect(status().isNoContent())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value("Api"));

        assertThat(userRepository.findByEmail(EMAIL).orElseThrow().getLastLoginAt()).isNotNull();
    }

    @Test
    void login_withRememberMe_setsRememberMeCookie() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", EMAIL).param("password", PASSWORD).param("remember-me", "true"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("remember-me"));
    }

    @Test
    void logout_returns204AndInvalidatesSession() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", EMAIL).param("password", PASSWORD))
                .andExpect(status().isNoContent())
                .andReturn().getRequest().getSession(false);

        mockMvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    // ---- routing -------------------------------------------------------------

    @Test
    @WithMockUser(EMAIL)
    void unknownApiPath_whenAuthenticated_returns404AndNeverTheSpaShell() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<html"))));
    }

    @Test
    void clientSideRoutes_areNotProtectedByTheApiSecurityRules() throws Exception {
        for (String path : new String[]{"/month", "/login", "/settings"}) {
            int status = mockMvc.perform(get(path).accept(MediaType.TEXT_HTML))
                    .andReturn().getResponse().getStatus();
            // 200 when the SPA bundle (index.html) is on the classpath, 404 otherwise; never 401/403
            assertThat(status).as(path).isIn(200, 404);
        }
    }
}
