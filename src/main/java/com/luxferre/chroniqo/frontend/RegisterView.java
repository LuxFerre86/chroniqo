package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.RegistrationDisabledException;
import com.luxferre.chroniqo.service.user.UserService;
import com.luxferre.chroniqo.util.PasswordValidator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Data;
import org.springframework.stereotype.Component;

@Route("register")
@PageTitle("Sign Up | chroniqo")
@AnonymousAllowed
@UIScope
@Component
public class RegisterView extends VerticalLayout {

    private final UserService userService;
    private final Binder<RegistrationForm> binder = new Binder<>(RegistrationForm.class);

    private final TextField firstNameField = new TextField("First Name");
    private final TextField lastNameField = new TextField("Last Name");
    private final EmailField emailField = new EmailField("Email");
    private final PasswordField passwordField = new PasswordField("Password");
    private final PasswordField confirmPasswordField = new PasswordField("Confirm Password");

    public RegisterView(UserService userService) {
        this.userService = userService;

        addClassName("register-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Background styling
        getStyle()
                .set("background", "linear-gradient(160deg, hsl(215, 25%, 9%) 0%, hsl(215, 25%, 7%) 50%, hsl(215, 25%, 9%) 100%)")
                .set("background-attachment", "fixed");

        // Register Container
        VerticalLayout registerContainer = new VerticalLayout();
        registerContainer.setMaxWidth("420px");
        registerContainer.setPadding(true);
        registerContainer.setSpacing(true);
        registerContainer.addClassName("register-container");
        registerContainer.getStyle()
                .set("background", "linear-gradient(145deg, hsl(215, 22%, 12%) 0%, hsl(215, 22%, 10%) 100%)")
                .set("border", "1px solid hsla(38, 40%, 50%, 0.12)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 20px 60px rgba(0, 0, 0, 0.8), inset 0 1px 0 hsla(38, 50%, 70%, 0.05)")
                .set("padding", "3rem 2.5rem");

        // Logo & Title
        Span logo = new Span("⏱");
        logo.getStyle()
                .set("font-size", "48px")
                .set("text-align", "center")
                .set("display", "block")
                .set("margin-bottom", "1rem");

        H1 title = new H1("Create Account");
        title.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700")
                .set("margin", "0 0 0.5rem 0")
                .set("text-align", "center")
                .set("color", "hsl(38, 95%, 65%)")
                .set("text-shadow", "0 2px 8px hsla(38, 92%, 50%, 0.4)");

        Span subtitle = new Span("Start tracking your time today");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("display", "block")
                .set("margin-bottom", "2rem")
                .set("font-size", "14px");

        // Form Fields
        firstNameField.setRequired(true);
        firstNameField.setWidthFull();

        lastNameField.setRequired(true);
        lastNameField.setWidthFull();

        emailField.setRequired(true);
        emailField.setWidthFull();
        emailField.setErrorMessage("Please enter a valid email address");

        passwordField.setRequired(true);
        passwordField.setWidthFull();
        passwordField.setHelperText(PasswordValidator.HELPER_TEXT);

        confirmPasswordField.setRequired(true);
        confirmPasswordField.setWidthFull();

        // Validation
        setupValidation();

        // Register Button
        Button registerButton = new Button("Create Account");
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();
        registerButton.getStyle()
                .set("margin-top", "1rem");

        registerButton.addClickListener(e -> handleRegistration());

        // Login Link
        Span loginText = new Span("Already have an account? ");
        loginText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("margin-top", "2rem");

        Button loginButton = new Button("Sign in", e ->
                UI.getCurrent().navigate("login")
        );
        loginButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        loginButton.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "600");

        VerticalLayout loginSection = new VerticalLayout(loginText, loginButton);
        loginSection.setPadding(false);
        loginSection.setSpacing(false);
        loginSection.setAlignItems(Alignment.CENTER);

        registerContainer.add(
                logo,
                title,
                subtitle,
                firstNameField,
                lastNameField,
                emailField,
                passwordField,
                confirmPasswordField,
                registerButton,
                loginSection
        );

        add(registerContainer);
    }

    private void setupValidation() {
        binder.forField(firstNameField)
                .asRequired("First name is required")
                .bind(RegistrationForm::getFirstName, RegistrationForm::setFirstName);

        binder.forField(lastNameField)
                .asRequired("Last name is required")
                .bind(RegistrationForm::getLastName, RegistrationForm::setLastName);

        binder.forField(emailField)
                .asRequired("Email is required")
                .withValidator(new EmailValidator("Please enter a valid email address"))
                .bind(RegistrationForm::getEmail, RegistrationForm::setEmail);

        binder.forField(passwordField)
                .asRequired("Password is required")
                .withValidator(new PasswordValidator())
                .bind(RegistrationForm::getPassword, RegistrationForm::setPassword);

        binder.forField(confirmPasswordField)
                .asRequired("Please confirm your password")
                .withValidator(confirmPassword ->
                                confirmPassword.equals(passwordField.getValue()),
                        "Passwords do not match")
                .bind(RegistrationForm::getConfirmPassword, RegistrationForm::setConfirmPassword);
    }

    private void handleRegistration() {
        RegistrationForm form = new RegistrationForm();

        if (binder.writeBeanIfValid(form)) {
            try {
                User ignored = userService.register(
                        form.getEmail(),
                        form.getPassword(),
                        form.getFirstName(),
                        form.getLastName()
                );

                Notification.show(
                        "Registration successful! Please check your email to verify your account.",
                        5000,
                        Notification.Position.TOP_CENTER
                ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                UI.getCurrent().navigate("login");

            } catch (RegistrationDisabledException e) {
                Notification.show(
                        "Registration is currently disabled. Please contact the administrator.",
                        5000,
                        Notification.Position.MIDDLE
                ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (IllegalArgumentException e) {
                Notification.show(
                        e.getMessage(),
                        5000,
                        Notification.Position.MIDDLE
                ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } else {
            Notification.show(
                    "Please fix the errors in the form",
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // Helper class for form binding
    @Data
    public static class RegistrationForm {
        private String firstName;
        private String lastName;
        private String email;
        private String password;
        private String confirmPassword;
    }
}