package com.luxferre.chroniqo.frontend;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.QueryParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = LoginView.class)
class LoginViewTest extends SpringBrowserlessTest {

    @Nested
    @DisplayName("Layout & Structure")
    class LayoutAndStructure {

        @Test
        void onLoad_showsApplicationTitle() {
            navigate(LoginView.class);
            // $view() scopes to LoginView only — AppLayoutBasic also renders an H1 "chroniqo"
            assertThat($view(H1.class).withText("chroniqo").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_showsSubtitle() {
            navigate(LoginView.class);
            assertThat($(Span.class).withText("Track your time with ease").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_loginFormIsPresent() {
            navigate(LoginView.class);
            assertThat($(LoginForm.class).all()).isNotEmpty();
        }

        @Test
        void onLoad_forgotPasswordButtonIsVisible() {
            navigate(LoginView.class);
            assertThat($(LoginForm.class).single().isForgotPasswordButtonVisible()).isTrue();
        }

        @Test
        void onLoad_rememberMeCheckboxIsPresent() {
            navigate(LoginView.class);
            // withText() matches visible text nodes — use withCondition for Checkbox labels
            assertThat($(Checkbox.class).withCondition(cb -> "Remember me".equals(cb.getLabel())).all()).isNotEmpty();
        }

        @Test
        void onLoad_signUpButtonIsUsable() {
            navigate(LoginView.class);
            assertThat(test($(Button.class).withText("Sign up").single()).isUsable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Before Enter Behavior")
    class BeforeEnterBehavior {

        @Test
        void withoutErrorParam_loginFormHasNoError() {
            navigate(LoginView.class);
            assertThat($(LoginForm.class).single().isError()).isFalse();
        }

        @Test
        void withErrorParam_loginFormShowsError() {
            UI.getCurrent().navigate(LoginView.class,
                    new QueryParameters(Map.of("error", List.of(""))));
            assertThat($(LoginForm.class).single().isError()).isTrue();
        }

        @Test
        void withAccountDeletedParam_showsSuccessNotification() {
            UI.getCurrent().navigate(LoginView.class,
                    new QueryParameters(Map.of("accountDeleted", List.of(""))));
            assertThat(test($(Notification.class).single()).getText())
                    .contains("Your account was deleted successfully.");
        }
    }

    @Nested
    @DisplayName("Navigation")
    class Navigation {

        @Test
        void clickForgotPassword_navigatesToPasswordResetRequestView() {
            navigate(LoginView.class);
            test($(LoginForm.class).single()).forgotPassword();
            assertThat(getCurrentView()).isInstanceOf(PasswordResetRequestView.class);
        }

        @Test
        void clickSignUp_navigatesToRegisterView() {
            navigate(LoginView.class);
            test($(Button.class).withText("Sign up").single()).click();
            assertThat(getCurrentView()).isInstanceOf(RegisterView.class);
        }
    }
}
