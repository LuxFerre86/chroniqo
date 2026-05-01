package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.service.user.UserService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.QueryParameters;
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
@ViewPackages(classes = PasswordResetConfirmView.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordResetConfirmViewTest extends SpringBrowserlessTest {

    /**
     * Navigates to the reset-password-confirm route with a token query parameter.
     * Consistent with the HasUrlParameter contract: token comes from query string,
     * not the path segment.
     */
    private void navigateWithToken(String token) {
        UI.getCurrent().navigate(
                PasswordResetConfirmView.class,
                new QueryParameters(Map.of("token", List.of(token)))
        );
    }

    /**
     * Navigates to the reset-password-confirm route without any query parameters,
     * triggering the no-token branch in setParameter.
     */
    private void navigateWithoutToken() {
        UI.getCurrent().navigate(
                PasswordResetConfirmView.class,
                new QueryParameters(Map.of())
        );
    }

    @Nested
    @DisplayName("Layout & Structure")
    class LayoutAndStructure {

        @Test
        void onLoad_showsTitle() {
            navigateWithToken("some-token");
            assertThat($(H1.class).withText("Set New Password").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_showsSubtitle() {
            navigateWithToken("some-token");
            assertThat($(Span.class).withText("Choose a strong password").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_newPasswordFieldIsPresent() {
            navigateWithToken("some-token");
            assertThat($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).all()).isNotEmpty();
        }

        @Test
        void onLoad_confirmPasswordFieldIsPresent() {
            navigateWithToken("some-token");
            assertThat($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).all()).isNotEmpty();
        }

        @Test
        void onLoad_saveButtonIsUsable() {
            navigateWithToken("some-token");
            assertThat(test($(Button.class).withText("Set New Password").single()).isUsable()).isTrue();
        }
    }

    @Nested
    @DisplayName("No Token in URL")
    class NoToken {

        @Test
        void withoutToken_showsErrorNotification() {
            navigateWithoutToken();
            assertThat(test($(Notification.class).single()).getText()).contains("Invalid reset link");
        }

        @Test
        void withoutToken_navigatesToLoginView() {
            navigateWithoutToken();
            assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        }
    }

    @Nested
    @DisplayName("Successful Password Reset")
    class SuccessfulReset {

        @MockitoBean
        private UserService userService;

        @Test
        void validTokenAndMatchingPasswords_showsSuccessNotification() {
            when(userService.resetPassword(anyString(), anyString())).thenReturn(true);
            navigateWithToken("valid-token");

            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single())
                    .setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single())
                    .setValue("SecurePass1!");
            test($(Button.class).withText("Set New Password").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("Password reset successful");
        }

        @Test
        void validTokenAndMatchingPasswords_navigatesToLoginView() {
            when(userService.resetPassword(anyString(), anyString())).thenReturn(true);
            navigateWithToken("valid-token");

            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single())
                    .setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single())
                    .setValue("SecurePass1!");
            test($(Button.class).withText("Set New Password").single()).click();

            assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        }
    }

    @Nested
    @DisplayName("Failed Password Reset")
    class FailedReset {

        @MockitoBean
        private UserService userService;

        @Test
        void expiredToken_showsErrorNotification() {
            when(userService.resetPassword(anyString(), anyString())).thenReturn(false);
            navigateWithToken("expired-token");

            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single())
                    .setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single())
                    .setValue("SecurePass1!");
            test($(Button.class).withText("Set New Password").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("invalid or expired");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void mismatchingPasswords_doesNotSubmitForm() {
            navigateWithToken("valid-token");

            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single())
                    .setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single())
                    .setValue("DifferentPass1!");
            test($(Button.class).withText("Set New Password").single()).click();

            assertThat($(Notification.class).all()).isEmpty();
        }

        @Test
        void weakPassword_doesNotSubmitForm() {
            navigateWithToken("valid-token");

            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single())
                    .setValue("weak");
            test($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single())
                    .setValue("weak");
            test($(Button.class).withText("Set New Password").single()).click();

            assertThat($(Notification.class).all()).isEmpty();
        }

        @Test
        void emptyFields_doesNotSubmitForm() {
            navigateWithToken("valid-token");
            test($(Button.class).withText("Set New Password").single()).click();

            assertThat($(Notification.class).all()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validation Messages")
    class ValidationMessages {

        @Test
        void emptyPassword_marksNewPasswordFieldAsInvalid() {
            navigateWithToken("valid-token");
            test($(Button.class).withText("Set New Password").single()).click();

            PasswordField newPasswordField = $(PasswordField.class)
                    .withCondition(f -> "New Password".equals(f.getLabel())).single();
            assertThat(newPasswordField.isInvalid()).isTrue();
        }

        @Test
        void emptyPassword_showsRequiredErrorOnNewPasswordField() {
            navigateWithToken("valid-token");
            test($(Button.class).withText("Set New Password").single()).click();

            PasswordField newPasswordField = $(PasswordField.class)
                    .withCondition(f -> "New Password".equals(f.getLabel())).single();
            assertThat(newPasswordField.getErrorMessage()).contains("Password is required");
        }

        @Test
        void shortPassword_marksNewPasswordFieldAsInvalid() {
            navigateWithToken("valid-token");
            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single())
                    .setValue("Short1!");
            test($(Button.class).withText("Set New Password").single()).click();

            PasswordField newPasswordField = $(PasswordField.class)
                    .withCondition(f -> "New Password".equals(f.getLabel())).single();
            assertThat(newPasswordField.isInvalid()).isTrue();
        }

        @Test
        void mismatchingPasswords_marksConfirmFieldAsInvalid() {
            navigateWithToken("valid-token");
            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single())
                    .setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single())
                    .setValue("DifferentPass1!");
            test($(Button.class).withText("Set New Password").single()).click();

            PasswordField confirmField = $(PasswordField.class)
                    .withCondition(f -> "Confirm Password".equals(f.getLabel())).single();
            assertThat(confirmField.isInvalid()).isTrue();
        }

        @Test
        void mismatchingPasswords_showsMismatchErrorMessageOnConfirmField() {
            navigateWithToken("valid-token");
            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single())
                    .setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single())
                    .setValue("DifferentPass1!");
            test($(Button.class).withText("Set New Password").single()).click();

            PasswordField confirmField = $(PasswordField.class)
                    .withCondition(f -> "Confirm Password".equals(f.getLabel())).single();
            assertThat(confirmField.getErrorMessage()).contains("Passwords do not match");
        }
    }
}
