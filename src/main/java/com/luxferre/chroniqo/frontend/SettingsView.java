package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.CountrySubdivisionRegistry;
import com.luxferre.chroniqo.service.user.UserService;
import com.luxferre.chroniqo.util.PasswordValidator;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Authenticated settings view ({@code /settings}) that lets the current user
 * update their profile (name, weekly target hours, working days, country and
 * subdivision), change their password, and review account metadata.
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
@Route(value = "settings", layout = AppLayoutBasic.class)
@PageTitle("Settings | chroniqo")
@UIScope
@Component
@RolesAllowed("ROLE_USER")
@Slf4j
public class SettingsView extends VerticalLayout {

    /**
     * Service for user-related operations (fetching current user, updating profile, changing password).
     */
    private final UserService userService;

    /**
     * Registry that provides mappings for countries and their subdivisions.
     */
    private final CountrySubdivisionRegistry countryRegistry;

    /**
     * Currently authenticated user; populated in constructor.
     */
    private final User currentUser;

    // Profile Section UI components

    /**
     * Text field for the user's first name.
     */
    private final TextField firstNameField = new TextField("First Name");

    /**
     * Text field for the user's last name.
     */
    private final TextField lastNameField = new TextField("Last Name");

    /**
     * Read-only email field showing the user's email.
     */
    private final EmailField emailField = new EmailField("Email");

    /**
     * Integer field to set weekly target hours (0-80).
     */
    private final IntegerField weeklyTargetHoursField =
            new IntegerField("Weekly Target Hours");

    /**
     * Checkbox group to select working days of the week.
     */
    private final CheckboxGroup<DayOfWeek> workingDaysField =
            new CheckboxGroup<>("Working Days");

    /**
     * Combo box for country selection.
     * Uses Map.Entry\<countryCode, countryName\> as item type so both code and display name are available.
     */
    private final ComboBox<Map.Entry<String, String>> countryField =
            new ComboBox<>("Country");

    /**
     * Combo box for subdivision (state/region) selection.
     * Populated dynamically based on selected country.
     */
    private final ComboBox<Map.Entry<String, String>> subdivisionField =
            new ComboBox<>("State / Region");

    /**
     * Button to save profile changes.
     */
    private final Button saveProfileButton = new Button("Save Changes");

    // Password Section UI components

    /**
     * Field for entering the current password when changing password.
     */
    private final PasswordField currentPasswordField =
            new PasswordField("Current Password");

    /**
     * Field for entering the new password when changing password.
     */
    private final PasswordField newPasswordField = new PasswordField("New Password");

    /**
     * Field for confirming the new password.
     */
    private final PasswordField confirmPasswordField =
            new PasswordField("Confirm New Password");

    /**
     * Button to trigger password change.
     */
    private final Button changePasswordButton = new Button("Change Password");

    /**
     * Binder used to validate and bind the password change form fields.
     * Uses the inner PasswordChangeForm DTO.
     */
    private final Binder<PasswordChangeForm> passwordBinder =
            new Binder<>(PasswordChangeForm.class);

    /**
     * Construct the settings view.
     *
     * @param userService     registry and actions for users
     * @param countryRegistry provider for country and subdivision data
     * @throws IllegalStateException if there is no currently authenticated user
     */
    public SettingsView(UserService userService,
                        CountrySubdivisionRegistry countryRegistry) {
        this.userService = userService;
        this.countryRegistry = countryRegistry;

        addClassName("settings-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        this.currentUser = Optional.ofNullable(userService.getCurrentUser())
                .orElseThrow(() -> new IllegalStateException("No user logged in"));

        H2 pageTitle = new H2("Settings");
        pageTitle.getStyle()
                .set("margin", "0 0 2rem 0")
                .set("color", "hsl(38, 95%, 65%)")
                .set("font-weight", "700");

        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setMaxWidth("800px");
        mainContainer.setPadding(false);
        mainContainer.setSpacing(true);
        mainContainer.getStyle().set("gap", "2rem");

        mainContainer.add(
                createProfileSection(),
                createPasswordSection(),
                createAccountSection());

        add(pageTitle, mainContainer);
    }

    /**
     * Build and return the "Profile Information" section layout.
     * This initializes fields with the current user's data and wires country/subdivision behavior.
     *
     * @return configured VerticalLayout representing the profile card
     */
    private VerticalLayout createProfileSection() {
        VerticalLayout section = sectionCard();

        H3 sectionTitle = sectionHeading("Profile Information");

        firstNameField.setValue(currentUser.getFirstName());
        firstNameField.setWidthFull();

        lastNameField.setValue(currentUser.getLastName());
        lastNameField.setWidthFull();

        emailField.setValue(currentUser.getEmail());
        emailField.setWidthFull();
        emailField.setReadOnly(true);
        emailField.setHelperText("Email cannot be changed");

        weeklyTargetHoursField.setValue(currentUser.getWeeklyTargetHours());
        weeklyTargetHoursField.setWidthFull();
        weeklyTargetHoursField.setMin(0);
        weeklyTargetHoursField.setMax(80);
        weeklyTargetHoursField.setStepButtonsVisible(true);
        weeklyTargetHoursField.setHelperText("0–80 hours per week (0 = no target)");

        // Working Days
        workingDaysField.setItems(DayOfWeek.values());
        workingDaysField.setItemLabelGenerator(day ->
                day.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        workingDaysField.setValue(currentUser.getWorkingDaysOrDefault());
        workingDaysField.setWidthFull();
        workingDaysField.setHelperText("Select the days you are scheduled to work");

        // Country
        List<Map.Entry<String, String>> countryEntries =
                new ArrayList<>(countryRegistry.getAllCountries().entrySet());
        countryField.setItems(countryEntries);
        countryField.setItemLabelGenerator(Map.Entry::getValue);
        countryField.setPlaceholder("None — disable automatic public holidays");
        countryField.setClearButtonVisible(true);
        countryField.setWidthFull();
        countryField.setHelperText(
                "Used for automatic public holiday detection");

        // Subdivision
        subdivisionField.setPlaceholder("Select your state / region (optional)");
        subdivisionField.setClearButtonVisible(true);
        subdivisionField.setWidthFull();
        subdivisionField.setVisible(false);

        // Wire country → subdivision
        countryField.addValueChangeListener(e -> updateSubdivisionField(e.getValue()));

        // Restore saved values
        restoreCountrySelection();

        saveProfileButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveProfileButton.addClickListener(e -> saveProfile());

        section.add(sectionTitle, firstNameField, lastNameField, countryField, subdivisionField, emailField,
                weeklyTargetHoursField, workingDaysField,
                saveProfileButton);
        return section;
    }

    /**
     * Update the subdivision field items and visibility based on the provided country selection.
     *
     * @param country the selected country entry, or null if cleared
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
     * Restore the user's previously saved country and subdivision selection in the UI.
     * Handles case where country or subdivision codes are null or not present in the registry.
     */
    private void restoreCountrySelection() {
        if (currentUser.getCountryCode() == null) return;

        countryField.getListDataView().getItems()
                .filter(e -> e.getKey().equalsIgnoreCase(currentUser.getCountryCode()))
                .findFirst()
                .ifPresent(entry -> {
                    countryField.setValue(entry);
                    // updateSubdivisionField fires via value-change listener;
                    // now restore the saved subdivision if any
                    if (currentUser.getSubdivisionCode() != null) {
                        subdivisionField.getListDataView().getItems()
                                .filter(s -> s.getKey().equalsIgnoreCase(
                                        currentUser.getSubdivisionCode()))
                                .findFirst()
                                .ifPresent(subdivisionField::setValue);
                    }
                });
    }

    /**
     * Build and return the "Change Password" section.
     * Sets up field properties and validation for the password change flow.
     *
     * @return configured VerticalLayout for password change
     */
    private VerticalLayout createPasswordSection() {
        VerticalLayout section = sectionCard();
        H3 sectionTitle = sectionHeading("Change Password");

        currentPasswordField.setWidthFull();
        currentPasswordField.setRequired(true);
        newPasswordField.setWidthFull();
        newPasswordField.setRequired(true);
        newPasswordField.setHelperText(PasswordValidator.HELPER_TEXT);
        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequired(true);

        setupPasswordValidation();

        changePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changePasswordButton.addClickListener(e -> changePassword());

        section.add(sectionTitle, currentPasswordField, newPasswordField,
                confirmPasswordField, changePasswordButton);
        return section;
    }

    /**
     * Build and return the "Account Information" section, showing member since and last login stats.
     *
     * @return configured VerticalLayout representing account metadata
     */
    private VerticalLayout createAccountSection() {
        VerticalLayout section = sectionCard();
        H3 sectionTitle = sectionHeading("Account Information");

        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setWidthFull();
        statsLayout.setSpacing(true);
        statsLayout.add(
                createStatBox("Member Since",
                        currentUser.getCreatedAt() != null
                                ? currentUser.getCreatedAt().toLocalDate().toString()
                                : "Unknown"),
                createStatBox("Last Login",
                        currentUser.getLastLoginAt() != null
                                ? currentUser.getLastLoginAt().toLocalDate().toString()
                                : "Never"));

        section.add(sectionTitle, statsLayout);
        return section;
    }

    /**
     * Create a simple stat box used in the account section.
     *
     * @param label short label for the stat (e.g. "Member Since")
     * @param value string value to display (e.g. a date)
     * @return VerticalLayout representing the stat box
     */
    private VerticalLayout createStatBox(String label, String value) {
        VerticalLayout box = new VerticalLayout();
        box.setPadding(true);
        box.setSpacing(false);
        box.getStyle()
                .set("background", "hsla(38, 30%, 50%, 0.05)")
                .set("border-radius", "8px")
                .set("padding", "1rem")
                .set("flex", "1");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "12px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px")
                .set("font-weight", "600");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "18px")
                .set("color", "var(--lumo-body-text-color)")
                .set("font-weight", "600")
                .set("margin-top", "0.5rem");

        box.add(labelSpan, valueSpan);
        return box;
    }

    /**
     * Configure validation rules for the password change form using the binder.
     * Validates presence, password strength, and that the confirmation matches.
     */
    private void setupPasswordValidation() {
        passwordBinder.forField(currentPasswordField)
                .asRequired("Current password is required")
                .bind(PasswordChangeForm::getCurrentPassword,
                        PasswordChangeForm::setCurrentPassword);

        passwordBinder.forField(newPasswordField)
                .asRequired("New password is required")
                .withValidator(new PasswordValidator())
                .bind(PasswordChangeForm::getNewPassword,
                        PasswordChangeForm::setNewPassword);

        passwordBinder.forField(confirmPasswordField)
                .asRequired("Please confirm your new password")
                .withValidator(confirm -> confirm.equals(newPasswordField.getValue()),
                        "Passwords do not match")
                .bind(PasswordChangeForm::getConfirmPassword,
                        PasswordChangeForm::setConfirmPassword);
    }

    /**
     * Validate and persist profile changes via {@link UserService}.
     * Performs client-side validations and shows notifications on success/failure.
     */
    private void saveProfile() {
        Integer weeklyHours = weeklyTargetHoursField.getValue();
        if (weeklyHours == null || weeklyHours < 0 || weeklyHours > 80) {
            Notification.show("Weekly target hours must be between 0 and 80.",
                            4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Set<DayOfWeek> selectedDays = workingDaysField.getValue();
        if (selectedDays == null || selectedDays.isEmpty()) {
            Notification.show("Please select at least one working day.",
                            4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        String countryCode = countryField.getValue() != null
                ? countryField.getValue().getKey() : null;
        String subdivisionCode =
                subdivisionField.isVisible() && subdivisionField.getValue() != null
                        ? subdivisionField.getValue().getKey() : null;

        try {
            userService.updateProfile(
                    currentUser.getEmail(),
                    firstNameField.getValue(),
                    lastNameField.getValue(),
                    weeklyHours,
                    selectedDays,
                    countryCode,
                    subdivisionCode);

            currentUser.setFirstName(firstNameField.getValue());
            currentUser.setLastName(lastNameField.getValue());
            currentUser.setWeeklyTargetHours(weeklyHours);
            currentUser.setWorkingDays(selectedDays);
            currentUser.setCountryCode(countryCode);
            currentUser.setSubdivisionCode(subdivisionCode);

            Notification.show("Profile updated successfully!",
                            3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception e) {
            log.error("Failed to update profile for user", e);
            Notification.show("Failed to update profile. Please try again.",
                            5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Attempt to change the user's password using the validated binder values.
     * Displays notifications for success and handles IllegalArgumentException from the service.
     */
    private void changePassword() {
        PasswordChangeForm form = new PasswordChangeForm();
        if (passwordBinder.writeBeanIfValid(form)) {
            try {
                userService.changePassword(currentUser.getEmail(),
                        form.getCurrentPassword(), form.getNewPassword());

                Notification.show("Password changed successfully!",
                                3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();

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

    // -------------------------------------------------------------------------
    // Layout helpers
    // -------------------------------------------------------------------------

    /**
     * Create a stylized section card layout used across the settings view.
     *
     * @return configured VerticalLayout for a card-like section
     */
    private VerticalLayout sectionCard() {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(true);
        section.setSpacing(true);
        section.addClassName("settings-section");
        section.getStyle()
                .set("background", "linear-gradient(145deg, hsl(215, 22%, 12%) 0%, hsl(215, 22%, 10%) 100%)")
                .set("border", "1px solid hsla(38, 40%, 50%, 0.12)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 12px rgba(0, 0, 0, 0.5)")
                .set("padding", "1.5rem");
        return section;
    }

    /**
     * Convenience helper to create a consistent H3 section heading with styling.
     *
     * @param text heading text
     * @return styled H3 element
     */
    private H3 sectionHeading(String text) {
        H3 h = new H3(text);
        h.getStyle()
                .set("margin", "0 0 1rem 0")
                .set("color", "var(--lumo-body-text-color)")
                .set("font-size", "20px");
        return h;
    }

    /**
     * DTO used by the password binder to collect and validate the password change form fields.
     */
    @Data
    public static class PasswordChangeForm {
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;
    }
}