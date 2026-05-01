package com.luxferre.chroniqo.frontend;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = PasswordResetRequestView.class)
class PasswordResetRequestViewTest extends SpringBrowserlessTest {

    @Nested
    @DisplayName("Layout & Structure")
    class LayoutAndStructure {

        @Test
        void onLoad_showsTitle() {
            navigate(PasswordResetRequestView.class);
            assertThat($(H1.class).withText("Reset Password").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_showsSubtitle() {
            navigate(PasswordResetRequestView.class);
            assertThat($(Span.class).withText("Enter your email and we'll send you a reset link").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_emailFieldIsPresent() {
            navigate(PasswordResetRequestView.class);
            assertThat($(EmailField.class).all()).isNotEmpty();
        }

        @Test
        void onLoad_sendResetLinkButtonIsUsable() {
            navigate(PasswordResetRequestView.class);
            assertThat(test($(Button.class).withText("Send Reset Link").single()).isUsable()).isTrue();
        }

        @Test
        void onLoad_backToLoginButtonIsUsable() {
            navigate(PasswordResetRequestView.class);
            assertThat(test($(Button.class).withText("Back to Login").single()).isUsable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validation Messages")
    class ValidationMessages {

        @Test
        void emptyEmail_marksEmailFieldAsInvalid() {
            navigate(PasswordResetRequestView.class);
            test($(Button.class).withText("Send Reset Link").single()).click();

            assertThat($(EmailField.class).single().isInvalid()).isTrue();
        }

        @Test
        void emptyEmail_showsRequiredErrorOnEmailField() {
            navigate(PasswordResetRequestView.class);
            test($(Button.class).withText("Send Reset Link").single()).click();

            assertThat($(EmailField.class).single().getErrorMessage()).contains("Email is required");
        }

        @Test
        void emptyEmail_doesNotSubmitForm() {
            navigate(PasswordResetRequestView.class);
            test($(Button.class).withText("Send Reset Link").single()).click();

            assertThat($(Notification.class).all()).isEmpty();
        }

        @Test
        void invalidEmailFormat_marksEmailFieldAsInvalid() {
            navigate(PasswordResetRequestView.class);
            test($(EmailField.class).single()).setValue("notanemail");
            test($(Button.class).withText("Send Reset Link").single()).click();

            assertThat($(EmailField.class).single().isInvalid()).isTrue();
        }

        @Test
        void invalidEmailFormat_doesNotSubmitForm() {
            navigate(PasswordResetRequestView.class);
            test($(EmailField.class).single()).setValue("notanemail");
            test($(Button.class).withText("Send Reset Link").single()).click();

            assertThat($(Notification.class).all()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Successful Reset Request")
    class SuccessfulResetRequest {

        @Test
        void validEmail_showsSuccessNotification() {
            navigate(PasswordResetRequestView.class);
            test($(EmailField.class).single()).setValue("user@example.com");
            test($(Button.class).withText("Send Reset Link").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("If an account exists with this email, you'll receive a reset link shortly.");
        }

        @Test
        void validEmail_navigatesToLoginView() {
            navigate(PasswordResetRequestView.class);
            test($(EmailField.class).single()).setValue("user@example.com");
            test($(Button.class).withText("Send Reset Link").single()).click();

            assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        }
    }

    @Nested
    @DisplayName("Navigation")
    class Navigation {

        @Test
        void clickBackToLogin_navigatesToLoginView() {
            navigate(PasswordResetRequestView.class);
            test($(Button.class).withText("Back to Login").single()).click();

            assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        }
    }
}
