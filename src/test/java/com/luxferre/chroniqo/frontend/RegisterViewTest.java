package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.CountrySubdivisionRegistry;
import com.luxferre.chroniqo.service.RegistrationDisabledException;
import com.luxferre.chroniqo.service.user.UserService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = RegisterView.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RegisterViewTest extends SpringBrowserlessTest {

    @MockitoBean
    private CountrySubdivisionRegistry countryRegistry;

    @BeforeEach
    void setUp() {
        when(countryRegistry.getAllCountries()).thenReturn(Collections.emptyMap());
        when(countryRegistry.getSubdivisions(any())).thenReturn(Collections.emptyMap());
    }

    private void fillValidForm() {
        navigate(RegisterView.class);
        test($(TextField.class).withCondition(f -> "First Name".equals(f.getLabel())).single()).setValue("John");
        test($(TextField.class).withCondition(f -> "Last Name".equals(f.getLabel())).single()).setValue("Doe");
        test($(IntegerField.class).withCondition(f -> "Weekly Target Hours".equals(f.getLabel())).single()).setValue(40);
        test($(EmailField.class).single()).setValue("john@example.com");
        test($(PasswordField.class).withCondition(f -> "Password".equals(f.getLabel())).single()).setValue("SecurePass1!");
        test($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single()).setValue("SecurePass1!");
    }

    @Nested
    @DisplayName("Layout & Structure")
    class LayoutAndStructure {

        @Test
        void onLoad_showsTitle() {
            navigate(RegisterView.class);
            assertThat($(H1.class).withText("Create Account").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_showsSubtitle() {
            navigate(RegisterView.class);
            assertThat($(Span.class).withText("Start tracking your time today").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_createAccountButtonIsUsable() {
            navigate(RegisterView.class);
            assertThat(test($(Button.class).withText("Create Account").single()).isUsable()).isTrue();
        }

        @Test
        void onLoad_signInButtonIsUsable() {
            navigate(RegisterView.class);
            assertThat(test($(Button.class).withText("Sign in").single()).isUsable()).isTrue();
        }

        @Test
        void onLoad_emailFieldIsPresent() {
            navigate(RegisterView.class);
            assertThat($(EmailField.class).all()).isNotEmpty();
        }

        @Test
        void onLoad_passwordFieldIsPresent() {
            navigate(RegisterView.class);
            assertThat($(PasswordField.class).withCondition(f -> "Password".equals(f.getLabel())).all()).isNotEmpty();
        }

        @Test
        void onLoad_confirmPasswordFieldIsPresent() {
            navigate(RegisterView.class);
            assertThat($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).all()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Validation Messages")
    class ValidationMessages {

        @Test
        void emptyForm_showsValidationErrorNotification() {
            navigate(RegisterView.class);
            test($(Button.class).withText("Create Account").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("Please fix the errors in the form");
        }

        @Test
        void emptyFirstName_marksFirstNameFieldAsInvalid() {
            navigate(RegisterView.class);
            test($(Button.class).withText("Create Account").single()).click();

            assertThat($(TextField.class).withCondition(f -> "First Name".equals(f.getLabel())).single().isInvalid()).isTrue();
        }

        @Test
        void emptyFirstName_showsRequiredErrorOnFirstNameField() {
            navigate(RegisterView.class);
            test($(Button.class).withText("Create Account").single()).click();

            assertThat($(TextField.class).withCondition(f -> "First Name".equals(f.getLabel())).single().getErrorMessage())
                    .contains("First name is required");
        }

        @Test
        void emptyLastName_marksLastNameFieldAsInvalid() {
            navigate(RegisterView.class);
            test($(Button.class).withText("Create Account").single()).click();

            assertThat($(TextField.class).withCondition(f -> "Last Name".equals(f.getLabel())).single().isInvalid()).isTrue();
        }

        @Test
        void emptyLastName_showsRequiredErrorOnLastNameField() {
            navigate(RegisterView.class);
            test($(Button.class).withText("Create Account").single()).click();

            assertThat($(TextField.class).withCondition(f -> "Last Name".equals(f.getLabel())).single().getErrorMessage())
                    .contains("Last name is required");
        }

        @Test
        void emptyEmail_marksEmailFieldAsInvalid() {
            navigate(RegisterView.class);
            test($(Button.class).withText("Create Account").single()).click();

            assertThat($(EmailField.class).single().isInvalid()).isTrue();
        }

        @Test
        void emptyEmail_showsRequiredErrorOnEmailField() {
            navigate(RegisterView.class);
            test($(Button.class).withText("Create Account").single()).click();

            assertThat($(EmailField.class).single().getErrorMessage()).contains("Email is required");
        }

        @Test
        void invalidEmail_marksEmailFieldAsInvalid() {
            navigate(RegisterView.class);
            test($(EmailField.class).single()).setValue("notanemail");
            test($(Button.class).withText("Create Account").single()).click();

            assertThat($(EmailField.class).single().isInvalid()).isTrue();
        }

        @Test
        void emptyPassword_marksPasswordFieldAsInvalid() {
            navigate(RegisterView.class);
            test($(Button.class).withText("Create Account").single()).click();

            assertThat($(PasswordField.class).withCondition(f -> "Password".equals(f.getLabel())).single().isInvalid()).isTrue();
        }

        @Test
        void emptyPassword_showsRequiredErrorOnPasswordField() {
            navigate(RegisterView.class);
            test($(Button.class).withText("Create Account").single()).click();

            assertThat($(PasswordField.class).withCondition(f -> "Password".equals(f.getLabel())).single().getErrorMessage())
                    .contains("Password is required");
        }

        @Test
        void mismatchingPasswords_marksConfirmPasswordFieldAsInvalid() {
            navigate(RegisterView.class);
            test($(TextField.class).withCondition(f -> "First Name".equals(f.getLabel())).single()).setValue("John");
            test($(TextField.class).withCondition(f -> "Last Name".equals(f.getLabel())).single()).setValue("Doe");
            test($(IntegerField.class).withCondition(f -> "Weekly Target Hours".equals(f.getLabel())).single()).setValue(40);
            test($(EmailField.class).single()).setValue("john@example.com");
            test($(PasswordField.class).withCondition(f -> "Password".equals(f.getLabel())).single()).setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single()).setValue("DifferentPass1!");
            test($(Button.class).withText("Create Account").single()).click();

            assertThat($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single().isInvalid()).isTrue();
        }

        @Test
        void mismatchingPasswords_showsMismatchErrorOnConfirmPasswordField() {
            navigate(RegisterView.class);
            test($(TextField.class).withCondition(f -> "First Name".equals(f.getLabel())).single()).setValue("John");
            test($(TextField.class).withCondition(f -> "Last Name".equals(f.getLabel())).single()).setValue("Doe");
            test($(IntegerField.class).withCondition(f -> "Weekly Target Hours".equals(f.getLabel())).single()).setValue(40);
            test($(EmailField.class).single()).setValue("john@example.com");
            test($(PasswordField.class).withCondition(f -> "Password".equals(f.getLabel())).single()).setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single()).setValue("DifferentPass1!");
            test($(Button.class).withText("Create Account").single()).click();

            assertThat($(PasswordField.class).withCondition(f -> "Confirm Password".equals(f.getLabel())).single().getErrorMessage())
                    .contains("Passwords do not match");
        }
    }

    @Nested
    @DisplayName("Successful Registration")
    class SuccessfulRegistration {

        @MockitoBean
        private UserService userService;

        @Test
        void validForm_showsSuccessNotification() {
            when(userService.register(any())).thenReturn(new User());
            fillValidForm();
            test($(Button.class).withText("Create Account").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("Registration successful!");
        }

        @Test
        void validForm_navigatesToLoginView() {
            when(userService.register(any())).thenReturn(new User());
            fillValidForm();
            test($(Button.class).withText("Create Account").single()).click();

            assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @MockitoBean
        private UserService userService;

        @Test
        void registrationDisabled_showsErrorNotification() {
            when(userService.register(any())).thenThrow(new RegistrationDisabledException("Registration is disabled."));
            fillValidForm();
            test($(Button.class).withText("Create Account").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("Registration is currently disabled");
        }

        @Test
        void emailAlreadyUsed_showsErrorNotification() {
            when(userService.register(any())).thenThrow(new IllegalArgumentException("Email already in use."));
            fillValidForm();
            test($(Button.class).withText("Create Account").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("Email already in use.");
        }
    }

    @Nested
    @DisplayName("Navigation")
    class Navigation {

        @Test
        void clickSignIn_navigatesToLoginView() {
            navigate(RegisterView.class);
            test($(Button.class).withText("Sign in").single()).click();

            assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        }
    }
}
