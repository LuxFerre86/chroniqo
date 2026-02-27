package com.luxferre.chroniqo.frontend;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

@Route("login")
@PageTitle("Login | ChroniQo")
@AnonymousAllowed
@UIScope
@Component
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView() {
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Background styling
        getStyle()
                .set("background", "linear-gradient(160deg, hsl(215, 25%, 9%) 0%, hsl(215, 25%, 7%) 50%, hsl(215, 25%, 9%) 100%)")
                .set("background-attachment", "fixed");

        // Login Container
        VerticalLayout loginContainer = new VerticalLayout();
        loginContainer.setMaxWidth("420px");
        loginContainer.setPadding(true);
        loginContainer.setSpacing(true);
        loginContainer.addClassName("login-container");
        loginContainer.getStyle()
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

        H1 title = new H1("ChroniQo");
        title.getStyle()
                .set("font-size", "32px")
                .set("font-weight", "700")
                .set("margin", "0 0 0.5rem 0")
                .set("text-align", "center")
                .set("color", "hsl(38, 95%, 65%)")
                .set("text-shadow", "0 2px 8px hsla(38, 92%, 50%, 0.4)");

        Span subtitle = new Span("Track your time with ease");
        subtitle.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("display", "block")
                .set("margin-bottom", "2rem")
                .set("font-size", "14px");

        // Login Form
        login.setAction("login");
        login.setForgotPasswordButtonVisible(true);
        login.addForgotPasswordListener(e ->
                UI.getCurrent().navigate("reset-password")
        );

        addRememberMeCheckbox();

        // Register Link
        Span registerText = new Span("Don't have an account? ");
        registerText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("margin-top", "2rem");

        Button registerButton = new Button("Sign up", e ->
                UI.getCurrent().navigate("register")
        );
        registerButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        registerButton.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "600");

        VerticalLayout registerSection = new VerticalLayout(registerText, registerButton);
        registerSection.setPadding(false);
        registerSection.setSpacing(false);
        registerSection.setAlignItems(Alignment.CENTER);

        loginContainer.add(
                logo,
                title,
                subtitle,
                login,
                registerSection
        );

        add(loginContainer);
    }

    void addRememberMeCheckbox() {
        Checkbox rememberMe = new Checkbox("Remember me");
        rememberMe.getStyle()
                .set("margin", "1rem 0")
                .set("color", "var(--lumo-body-text-color)");
        rememberMe.getElement().setAttribute("name", "remember-me");
        Element loginFormElement = login.getElement();
        Element element = rememberMe.getElement();
        loginFormElement.appendChild(element);

        String executeJsForFieldString = "const field = document.getElementById($0);" +
                "if(field) {" +
                "   field.after($1)" +
                "} else {" +
                "   console.error('could not find field', $0);" +
                "}";
        getElement().executeJs(executeJsForFieldString, "vaadinLoginPassword", element);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Show error message if login failed
        if (event.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            login.setError(true);
        }
    }
}
