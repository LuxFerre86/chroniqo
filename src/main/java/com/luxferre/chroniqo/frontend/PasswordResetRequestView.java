package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.service.AuthenticationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Data;
import org.springframework.stereotype.Component;

@Route("reset-password")
@PageTitle("Reset Password | ChroniQo")
@AnonymousAllowed
@UIScope
@Component
public class PasswordResetRequestView extends VerticalLayout {

    private final AuthenticationService authService;
    private final Binder<ResetForm> binder = new Binder<>(ResetForm.class);

    public PasswordResetRequestView(AuthenticationService authService) {
        this.authService = authService;

        addClassName("reset-view");
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
        Span logo = new Span("🔑");
        logo.getStyle()
                .set("font-size", "48px")
                .set("text-align", "center")
                .set("display", "block")
                .set("margin-bottom", "1rem");

        // Title
        H1 title = new H1("Reset Password");
        title.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700")
                .set("margin", "0 0 0.5rem 0")
                .set("text-align", "center")
                .set("color", "hsl(38, 95%, 65%)");

        Span subtitle = new Span("Enter your email and we'll send you a reset link");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("display", "block")
                .set("margin-bottom", "2rem")
                .set("font-size", "14px");

        // Email Field
        EmailField emailField = new EmailField("Email");
        emailField.setRequired(true);
        emailField.setWidthFull();
        emailField.setPlaceholder("your@email.com");

        // Validation
        binder.forField(emailField)
                .asRequired("Email is required")
                .withValidator(new EmailValidator("Please enter a valid email"))
                .bind(ResetForm::getEmail, ResetForm::setEmail);

        // Reset Button
        Button resetButton = new Button("Send Reset Link");
        resetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        resetButton.setWidthFull();
        resetButton.getStyle().set("margin-top", "1rem");
        resetButton.addClickListener(e -> handleReset());

        // Back to Login
        Button backButton = new Button("Back to Login", e ->
                UI.getCurrent().navigate("login")
        );
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.setWidthFull();
        backButton.getStyle()
                .set("margin-top", "0.5rem")
                .set("color", "var(--lumo-secondary-text-color)");

        container.add(logo, title, subtitle, emailField, resetButton, backButton);
        add(container);
    }

    private void handleReset() {
        ResetForm form = new ResetForm();

        if (binder.writeBeanIfValid(form)) {
            authService.requestPasswordReset(form.getEmail());

            Notification.show(
                    "If an account exists with this email, you'll receive a reset link shortly.",
                    5000,
                    Notification.Position.TOP_CENTER
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            UI.getCurrent().navigate("login");
        }
    }

    @Data
    public static class ResetForm {
        private String email;
    }
}
