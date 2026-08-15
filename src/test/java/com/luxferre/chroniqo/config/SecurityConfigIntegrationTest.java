package com.luxferre.chroniqo.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityConfigIntegrationTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

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
        HttpResponse<Void> response = sendGet(path);
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/favicon.ico",
            "/sw.js"
    })
    void unauthenticatedPublicAssets_doNotReturnUnauthorized(String path) throws Exception {
        HttpResponse<Void> response = sendGet(path);
        assertThat(response.statusCode()).isNotEqualTo(401);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/auth/verify-email?token=invalid-token",
            "/api/auth/me"
    })
    void authEndpoints_keepExpectedAnonymousBehavior(String path) throws Exception {
        HttpResponse<Void> response = sendGet(path);
        if (path.equals("/api/auth/me")) {
            assertThat(response.statusCode()).isEqualTo(401);
            return;
        }
        assertThat(response.statusCode()).isEqualTo(400);
    }

    private HttpResponse<Void> sendGet(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }
}
