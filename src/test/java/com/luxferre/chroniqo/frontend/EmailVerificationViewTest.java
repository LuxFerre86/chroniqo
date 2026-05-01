package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.service.user.EmailVerificationResult;
import com.luxferre.chroniqo.service.user.UserService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.QueryParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = EmailVerificationView.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmailVerificationViewTest extends SpringBrowserlessTest {

    /**
     * Navigates to the verify-email route with the given token as a query parameter.
     * UI.navigate(String) rejects '?' in path strings, so QueryParameters must be used.
     */
    private void navigateWithToken(String token) {
        UI.getCurrent().navigate(
                EmailVerificationView.class,
                new QueryParameters(Map.of("token", List.of(token)))
        );
    }

    @Nested
    @DisplayName("No Token in URL")
    class NoToken {

        @Test
        void withoutToken_showsVerificationFailedTitle() {
            navigate(EmailVerificationView.class);
            assertThat($(H1.class).withText("Verification Failed").single().isVisible()).isTrue();
        }

        @Test
        void withoutToken_showsInvalidLinkMessage() {
            navigate(EmailVerificationView.class);
            assertThat($(Span.class).withTextContaining("invalid or has expired").single().isVisible()).isTrue();
        }

        @Test
        void withoutToken_goToLoginButtonIsUsable() {
            navigate(EmailVerificationView.class);
            assertThat(test($(Button.class).withText("Go to Login").single()).isUsable()).isTrue();
        }

        @Test
        void withoutToken_clickGoToLogin_navigatesToLoginView() {
            navigate(EmailVerificationView.class);
            test($(Button.class).withText("Go to Login").single()).click();
            assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        }
    }

    @Nested
    @DisplayName("Token → VERIFIED_LOGGED_IN")
    class VerifiedLoggedIn {

        @MockitoBean
        private UserService userService;

        @BeforeEach
        void setup() {
            when(userService.verifyEmail(anyString())).thenReturn(EmailVerificationResult.VERIFIED_LOGGED_IN);
        }

        @Test
        void verifiedLoggedIn_showsEmailVerifiedTitle() {
            navigateWithToken("valid-token");
            assertThat($(H1.class).withText("Email Verified!").single().isVisible()).isTrue();
        }

        @Test
        void verifiedLoggedIn_showsLoggedInMessage() {
            navigateWithToken("valid-token");
            assertThat($(Span.class).withTextContaining("You're now logged in").single().isVisible()).isTrue();
        }

        @Test
        void verifiedLoggedIn_goToDashboardButtonIsUsable() {
            navigateWithToken("valid-token");
            assertThat(test($(Button.class).withText("Go to Dashboard").single()).isUsable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Token → VERIFIED_LOGIN_REQUIRED")
    class VerifiedLoginRequired {

        @MockitoBean
        private UserService userService;

        @BeforeEach
        void setup() {
            when(userService.verifyEmail(anyString())).thenReturn(EmailVerificationResult.VERIFIED_LOGIN_REQUIRED);
        }

        @Test
        void verifiedLoginRequired_showsEmailVerifiedTitle() {
            navigateWithToken("valid-token");
            assertThat($(H1.class).withText("Email Verified!").single().isVisible()).isTrue();
        }

        @Test
        void verifiedLoginRequired_showsLoginRequiredMessage() {
            navigateWithToken("valid-token");
            assertThat($(Span.class).withTextContaining("Please log in to continue").single().isVisible()).isTrue();
        }

        @Test
        void verifiedLoginRequired_goToLoginButtonIsUsable() {
            navigateWithToken("valid-token");
            assertThat(test($(Button.class).withText("Go to Login").single()).isUsable()).isTrue();
        }

        @Test
        void verifiedLoginRequired_clickGoToLogin_navigatesToLoginView() {
            navigateWithToken("valid-token");
            test($(Button.class).withText("Go to Login").single()).click();
            assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        }
    }

    @Nested
    @DisplayName("Invalid Token")
    class InvalidToken {

        @Test
        void invalidToken_showsVerificationFailedTitle() {
            navigateWithToken("expired-token");
            assertThat($(H1.class).withText("Verification Failed").single().isVisible()).isTrue();
        }

        @Test
        void invalidToken_showsInvalidLinkMessage() {
            navigateWithToken("expired-token");
            assertThat($(Span.class).withTextContaining("invalid or has expired").single().isVisible()).isTrue();
        }

        @Test
        void invalidToken_goToLoginButtonIsUsable() {
            navigateWithToken("expired-token");
            assertThat(test($(Button.class).withText("Go to Login").single()).isUsable()).isTrue();
        }

        @Test
        void invalidToken_clickGoToLogin_navigatesToLoginView() {
            navigateWithToken("expired-token");
            test($(Button.class).withText("Go to Login").single()).click();
            assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        }
    }
}
