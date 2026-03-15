package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.service.user.EmailVerificationResult;
import com.luxferre.chroniqo.service.user.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Public view rendered when a user follows the email-verification link
 * ({@code /verify-email?token=...}).
 *
 * <p>Delegates token validation and auto-login to
 * {@link UserService#verifyEmail(String)}.
 * The rendered content depends on the
 * {@link EmailVerificationResult}:
 * a dashboard shortcut on successful login, a login redirect when the session
 * could not be established, or an error card for invalid/expired tokens.
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
@Route("verify-email")
@PageTitle("Verify Email | chroniqo")
@AnonymousAllowed
@UIScope
@Component
public class EmailVerificationView extends VerticalLayout implements HasUrlParameter<String> {

    private final UserService userService;
    private EmailVerificationResult verificationResult = EmailVerificationResult.INVALID;

    public EmailVerificationView(UserService userService) {
        this.userService = userService;

        addClassName("verification-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Background
        getStyle()
                .set("background", "linear-gradient(160deg, hsl(215, 25%, 9%) 0%, hsl(215, 25%, 7%) 50%, hsl(215, 25%, 9%) 100%)")
                .set("background-attachment", "fixed");
    }

    /**
     * Called by Vaadin with the URL path segment and query parameters.
     * Extracts the {@code token} query parameter and triggers email verification.
     *
     * @param event     the navigation event
     * @param parameter the optional path parameter (unused; token is a query param)
     */
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();

        if (params.containsKey("token")) {
            String token = params.get("token").getFirst();
            verificationResult = userService.verifyEmail(token);
            renderContent();
        } else {
            renderErrorContent();
        }
    }

    private void renderContent() {
        removeAll();

        VerticalLayout container = new VerticalLayout();
        container.setMaxWidth("420px");
        container.setPadding(true);
        container.setAlignItems(Alignment.CENTER);
        container.getStyle()
                .set("background", "linear-gradient(145deg, hsl(215, 22%, 12%) 0%, hsl(215, 22%, 10%) 100%)")
                .set("border", "1px solid hsla(38, 40%, 50%, 0.12)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 20px 60px rgba(0, 0, 0, 0.8)")
                .set("padding", "3rem 2.5rem")
                .set("text-align", "center");

        if (verificationResult == EmailVerificationResult.VERIFIED_LOGGED_IN) {
            // Success – session established, user is logged in
            Span logo = new Span("✅");
            logo.getStyle()
                    .set("font-size", "64px")
                    .set("display", "block")
                    .set("margin-bottom", "1.5rem");

            H1 title = new H1("Email Verified!");
            title.getStyle()
                    .set("font-size", "28px")
                    .set("font-weight", "700")
                    .set("margin", "0 0 1rem 0")
                    .set("color", "hsl(142, 75%, 55%)");

            Span message = new Span("Your email has been successfully verified. You're now logged in!");
            message.getStyle()
                    .set("color", "var(--lumo-body-text-color)")
                    .set("margin-bottom", "2rem")
                    .set("display", "block")
                    .set("line-height", "1.6");

            Button dashboardButton = new Button("Go to Dashboard", e ->
                    UI.getCurrent().navigate("")
            );
            dashboardButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            dashboardButton.getStyle().set("margin-top", "1rem");

            container.add(logo, title, message, dashboardButton);
        } else if (verificationResult == EmailVerificationResult.VERIFIED_LOGIN_REQUIRED) {
            // Account verified but session could not be persisted – prompt manual login
            Span logo = new Span("✅");
            logo.getStyle()
                    .set("font-size", "64px")
                    .set("display", "block")
                    .set("margin-bottom", "1.5rem");

            H1 title = new H1("Email Verified!");
            title.getStyle()
                    .set("font-size", "28px")
                    .set("font-weight", "700")
                    .set("margin", "0 0 1rem 0")
                    .set("color", "hsl(142, 75%, 55%)");

            Span message = new Span("Your email has been successfully verified. Please log in to continue.");
            message.getStyle()
                    .set("color", "var(--lumo-body-text-color)")
                    .set("margin-bottom", "2rem")
                    .set("display", "block")
                    .set("line-height", "1.6");

            Button loginButton = new Button("Go to Login", e ->
                    UI.getCurrent().navigate("login")
            );
            loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            loginButton.getStyle().set("margin-top", "1rem");

            container.add(logo, title, message, loginButton);
        } else {
            renderErrorContent();
            return;
        }

        add(container);
    }

    private void renderErrorContent() {
        removeAll();

        VerticalLayout container = new VerticalLayout();
        container.setMaxWidth("420px");
        container.setPadding(true);
        container.setAlignItems(Alignment.CENTER);
        container.getStyle()
                .set("background", "linear-gradient(145deg, hsl(215, 22%, 12%) 0%, hsl(215, 22%, 10%) 100%)")
                .set("border", "1px solid hsla(38, 40%, 50%, 0.12)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 20px 60px rgba(0, 0, 0, 0.8)")
                .set("padding", "3rem 2.5rem")
                .set("text-align", "center");

        // Error
        Span logo = new Span("❌");
        logo.getStyle()
                .set("font-size", "64px")
                .set("display", "block")
                .set("margin-bottom", "1.5rem");

        H1 title = new H1("Verification Failed");
        title.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700")
                .set("margin", "0 0 1rem 0")
                .set("color", "hsl(12, 90%, 65%)");

        Span message = new Span("The verification link is invalid or has expired. Please request a new verification email.");
        message.getStyle()
                .set("color", "var(--lumo-body-text-color)")
                .set("margin-bottom", "2rem")
                .set("display", "block")
                .set("line-height", "1.6");

        Button loginButton = new Button("Go to Login", e ->
                UI.getCurrent().navigate("login")
        );
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        container.add(logo, title, message, loginButton);
        add(container);
    }
}