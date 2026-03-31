package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.CountrySubdivisionRegistry;
import com.luxferre.chroniqo.service.RegistrationDisabledException;
import com.luxferre.chroniqo.service.user.UserService;
import com.luxferre.chroniqo.util.PasswordValidator;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Public registration view ({@code /register}) allowing new users to create an
 * account.
 *
 * <p>On successful form submission the account is created in a disabled state
 * and a verification email is sent. The user is redirected to the login page
 * with a prompt to check their inbox. Registration can be disabled globally via
 * the {@code app.registration.enabled} property, in which case an error
 * notification is shown.
 *
 * <p>Country and subdivision fields are optional and use the full ISO 3166-1 /
 * ISO 3166-2 datasets provided by {@link CountrySubdivisionRegistry}. When a
 * country is selected, the subdivision field becomes visible and is populated
 * with the matching ISO 3166-2 entries. Selecting a country and subdivision
 * enables automatic public holiday detection in the calendar.
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
@Route("register")
@PageTitle("Sign Up | chroniqo")
@AnonymousAllowed
@UIScope
@Component
public class RegisterView extends VerticalLayout {

    /**
     * Service for user-related operations such as registration.
     */
    private final UserService userService;

    /**
     * Registry that provides country and subdivision ISO 3166 data.
     */
    private final CountrySubdivisionRegistry countryRegistry;

    /**
     * Binder used to validate and bind form values to {@link RegistrationForm}.
     */
    private final Binder<RegistrationForm> binder = new Binder<>(RegistrationForm.class);

    /* --- Form fields --- */

    /**
     * Input for user's first name (required).
     */
    private final TextField firstNameField = new TextField("First Name");

    /**
     * Input for user's last name (required).
     */
    private final TextField lastNameField = new TextField("Last Name");

    /**
     * Numeric input for weekly target hours (0-80).
     */
    private final IntegerField weeklyTargetHoursField = new IntegerField("Weekly Target Hours");

    /**
     * Email input field (required, validated).
     */
    private final EmailField emailField = new EmailField("Email");

    /**
     * Password input field (required, validated by {@link PasswordValidator}).
     */
    private final PasswordField passwordField = new PasswordField("Password");

    /**
     * Confirm password input (must match {@link #passwordField}).
     */
    private final PasswordField confirmPasswordField = new PasswordField("Confirm Password");

    // Country / subdivision — populated from ISO 3166 data

    /**
     * ComboBox showing available countries as map entries (code -> name).
     */
    private final ComboBox<Map.Entry<String, String>> countryField = new ComboBox<>("Country");

    /**
     * ComboBox showing subdivisions (state/region) for the selected country.
     */
    private final ComboBox<Map.Entry<String, String>> subdivisionField = new ComboBox<>("State / Region");

    /**
     * Construct the registration view and initialize UI components, layout and
     * validation.
     *
     * @param userService     service responsible for creating accounts
     * @param countryRegistry registry providing ISO country/subdivision data
     */
    public RegisterView(UserService userService,
                        CountrySubdivisionRegistry countryRegistry) {
        this.userService = userService;
        this.countryRegistry = countryRegistry;

        addClassName("register-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        getStyle()
                .set("background", "linear-gradient(160deg, hsl(215, 25%, 9%) 0%, hsl(215, 25%, 7%) 50%, hsl(215, 25%, 9%) 100%)")
                .set("background-attachment", "fixed");

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

        Image logo = new Image("icons/icon-48x48.png", "Logo");
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

        firstNameField.setRequired(true);
        firstNameField.setWidthFull();

        lastNameField.setRequired(true);
        lastNameField.setWidthFull();

        weeklyTargetHoursField.setWidthFull();
        weeklyTargetHoursField.setMin(0);
        weeklyTargetHoursField.setMax(80);
        weeklyTargetHoursField.setStepButtonsVisible(true);
        weeklyTargetHoursField.setHelperText("0–80 hours per week (0 = no target)");

        emailField.setRequired(true);
        emailField.setWidthFull();
        emailField.setErrorMessage("Please enter a valid email address");

        passwordField.setRequired(true);
        passwordField.setWidthFull();
        passwordField.setHelperText(PasswordValidator.HELPER_TEXT);

        confirmPasswordField.setRequired(true);
        confirmPasswordField.setWidthFull();

        setupCountryFields();
        setupValidation();

        Button registerButton = new Button("Create Account");
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();
        registerButton.getStyle().set("margin-top", "1rem");
        registerButton.addClickListener(e -> handleRegistration());

        Span loginText = new Span("Already have an account? ");
        loginText.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-align", "center")
                .set("margin-top", "2rem");

        Button loginButton = new Button("Sign in", e -> UI.getCurrent().navigate("login"));
        loginButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        loginButton.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "600");

        VerticalLayout loginSection = new VerticalLayout(loginText, loginButton);
        loginSection.setPadding(false);
        loginSection.setSpacing(false);
        loginSection.setAlignItems(Alignment.CENTER);

        registerContainer.add(
                logo, title, subtitle,
                firstNameField, lastNameField, countryField, subdivisionField, weeklyTargetHoursField, emailField,
                passwordField, confirmPasswordField,
                registerButton, loginSection);

        add(registerContainer);
    }

    /**
     * Initialize country and subdivision combo boxes:
     * - Populates the country list from {@link CountrySubdivisionRegistry#getAllCountries()}.
     * - Configures placeholders, labels and visibility.
     * - Registers a listener to update subdivisions when a country is selected.
     */
    private void setupCountryFields() {
        // Country — full ISO 3166-1 list sorted by name
        List<Map.Entry<String, String>> countryEntries =
                new ArrayList<>(countryRegistry.getAllCountries().entrySet());

        countryField.setItems(countryEntries);
        countryField.setItemLabelGenerator(Map.Entry::getValue);
        countryField.setPlaceholder("Select your country (optional)");
        countryField.setClearButtonVisible(true);
        countryField.setWidthFull();
        countryField.setHelperText(
                "Used to automatically detect public holidays in your calendar");

        // Subdivision — hidden until a country with subdivisions is selected
        subdivisionField.setPlaceholder("Select your state / region (optional)");
        subdivisionField.setClearButtonVisible(true);
        subdivisionField.setWidthFull();
        subdivisionField.setVisible(false);

        countryField.addValueChangeListener(e -> updateSubdivisionField(e.getValue()));
    }

    /**
     * Update the subdivision field when a country is selected or cleared.
     * If the selected country has no subdivisions the subdivision field is hidden.
     *
     * @param country the selected country entry (code -> name), or {@code null} if cleared
     */
    private void updateSubdivisionField(Map.Entry<String, String> country) {
        subdivisionField.clear();
        if (country == null) {
            subdivisionField.setVisible(false);
            return;
        }
        Map<String, String> subs = countryRegistry.getSubdivisions(country.getKey());
        if (subs.isEmpty()) {
            subdivisionField.setVisible(false);
            return;
        }
        List<Map.Entry<String, String>> subEntries = new ArrayList<>(subs.entrySet());
        subdivisionField.setItems(subEntries);
        subdivisionField.setItemLabelGenerator(Map.Entry::getValue);
        subdivisionField.setVisible(true);
    }

    /**
     * Configure form validation rules and bind UI fields to {@link RegistrationForm} properties.
     * Rules include required checks, range validation for hours, email format validation and
     * password matching.
     */
    private void setupValidation() {
        binder.forField(firstNameField)
                .asRequired("First name is required")
                .bind(RegistrationForm::getFirstName, RegistrationForm::setFirstName);

        binder.forField(lastNameField)
                .asRequired("Last name is required")
                .bind(RegistrationForm::getLastName, RegistrationForm::setLastName);

        binder.forField(weeklyTargetHoursField)
                .asRequired("Weekly Target Hours are required")
                .withValidator(hours -> hours >= 0 && hours <= 80, "Please enter a value between 0 and 80")
                .bind(RegistrationForm::getWeeklyTargetHours, RegistrationForm::setWeeklyTargetHours);

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
                .withValidator(confirm -> confirm.equals(passwordField.getValue()),
                        "Passwords do not match")
                .bind(RegistrationForm::getConfirmPassword,
                        RegistrationForm::setConfirmPassword);
    }

    /**
     * Handle the registration button click:
     * - Validate the form and, if valid, call {@link UserService#register} to create the user.
     * - Extract country and subdivision codes from the selected items.
     * - Show a notification on success and navigate to the login page.
     * - Handle and display errors such as disabled registration or invalid input.
     */
    private void handleRegistration() {
        RegistrationForm form = new RegistrationForm();
        if (binder.writeBeanIfValid(form)) {
            try {
                String countryCode = countryField.getValue() != null
                        ? countryField.getValue().getKey() : null;
                String subdivisionCode =
                        subdivisionField.isVisible() && subdivisionField.getValue() != null
                                ? subdivisionField.getValue().getKey() : null;

                User ignored = userService.register(
                        form.getEmail(),
                        form.getPassword(),
                        form.getFirstName(),
                        form.getLastName(),
                        countryCode,
                        subdivisionCode);

                Notification.show(
                                "Registration successful! Please check your email to verify your account.",
                                5000,
                                Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                UI.getCurrent().navigate("login");

            } catch (RegistrationDisabledException e) {
                Notification.show(
                                "Registration is currently disabled. Please contact the administrator.",
                                5000,
                                Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (IllegalArgumentException e) {
                Notification.show(e.getMessage(), 5000,
                                Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } else {
            Notification.show("Please fix the errors in the form",
                            3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Simple DTO used by the binder to hold registration form values before submission.
     * Lombok's {@code @Data} provides getters/setters/equals/hashCode/toString.
     */
    @Data
    public static class RegistrationForm {
        /**
         * User's first name.
         */
        private String firstName;

        /**
         * User's last name.
         */
        private String lastName;

        /**
         * Weekly target hours, allowed range 0–80 (0 disables target).
         */
        private Integer weeklyTargetHours;

        /**
         * User's email address (login).
         */
        private String email;

        /**
         * Password chosen by the user.
         */
        private String password;

        /**
         * Confirmation of {@link #password}, must match.
         */
        private String confirmPassword;
    }
}