package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.service.user.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Route("reset-password-confirm")
@PageTitle("Set New Password | chroniqo")
@AnonymousAllowed
@UIScope
@Component
public class PasswordResetConfirmView extends VerticalLayout implements HasUrlParameter<String> {

    private final UserService userService;
    private final Binder<PasswordForm> binder = new Binder<>(PasswordForm.class);

    private String resetToken;
    private final PasswordField newPasswordField = new PasswordField("New Password");
    private final PasswordField confirmPasswordField = new PasswordField("Confirm Password");

    public PasswordResetConfirmView(UserService userService) {
        this.userService = userService;

        addClassName("reset-confirm-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Background
        getStyle()
                .set("background", "linear-gradient(160deg, hsl(215, 25%, 9%) 0%, hsl(215, 25%, 7%) 50%, hsl(215, 25%, 9%) 100%)")
                .set("background-attachment", "fixed");

        // Container
        VerticalLayout container = new VerticalLayout();
        container.setMaxWidth("420px");
        container.setPadding(true);
        container.getStyle()
                .set("background", "linear-gradient(145deg, hsl(215, 22%, 12%) 0%, hsl(215, 22%, 10%) 100%)")
                .set("border", "1px solid hsla(38, 40%, 50%, 0.12)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 20px 60px rgba(0, 0, 0, 0.8)")
                .set("padding", "3rem 2.5rem");

        // Logo
        Span logo = new Span("🔐");
        logo.getStyle()
                .set("font-size", "48px")
                .set("text-align", "center")
                .set("display", "block")
                .set("margin-bottom", "1rem");

        // Title
        H1 title = new H1("Set New Password");
        title.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700")
                .set("margin", "0 0 0.5rem 0")
                .set("text-align", "center")
                .set("color", "hsl(38, 95%, 65%)");

        Span subtitle = new Span("Choose a strong password");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("display", "block")
                .set("margin-bottom", "2rem")
                .set("font-size", "14px");

        // Password Fields
        newPasswordField.setRequired(true);
        newPasswordField.setWidthFull();
        newPasswordField.setHelperText("At least 8 characters");

        confirmPasswordField.setRequired(true);
        confirmPasswordField.setWidthFull();

        // Validation
        setupValidation();

        // Save Button
        Button saveButton = new Button("Set New Password");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setWidthFull();
        saveButton.getStyle().set("margin-top", "1rem");
        saveButton.addClickListener(e -> handlePasswordReset());

        container.add(logo, title, subtitle, newPasswordField, confirmPasswordField, saveButton);
        add(container);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();

        if (params.containsKey("token")) {
            this.resetToken = params.get("token").getFirst();
        } else {
            Notification.show(
                    "Invalid reset link",
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("login");
        }
    }

    private void setupValidation() {
        binder.forField(newPasswordField)
                .asRequired("Password is required")
                .withValidator(password -> password.length() >= 8,
                        "Password must be at least 8 characters")
                .bind(PasswordForm::getPassword, PasswordForm::setPassword);

        binder.forField(confirmPasswordField)
                .asRequired("Please confirm your password")
                .withValidator(confirmPassword ->
                                confirmPassword.equals(newPasswordField.getValue()),
                        "Passwords do not match")
                .bind(PasswordForm::getConfirmPassword, PasswordForm::setConfirmPassword);
    }

    private void handlePasswordReset() {
        PasswordForm form = new PasswordForm();

        if (binder.writeBeanIfValid(form)) {
            boolean success = userService.resetPassword(resetToken, form.getPassword());

            if (success) {
                Notification.show(
                        "Password reset successful! You can now log in.",
                        5000,
                        Notification.Position.TOP_CENTER
                ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                UI.getCurrent().navigate("login");
            } else {
                Notification.show(
                        "Reset link is invalid or expired. Please request a new one.",
                        5000,
                        Notification.Position.MIDDLE
                ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }

    @Data
    public static class PasswordForm {
        private String password;
        private String confirmPassword;
    }
}











