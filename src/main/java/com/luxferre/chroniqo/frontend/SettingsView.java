package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.user.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

import java.util.Optional;

@Route(value = "settings", layout = AppLayoutBasic.class)
@PageTitle("Settings | chroniqo")
@UIScope
@Component
@RolesAllowed("ROLE_USER")
@Slf4j
public class SettingsView extends VerticalLayout {

    private final UserService userService;
    private final User currentUser;

    // Profile Section
    private final TextField firstNameField = new TextField("First Name");
    private final TextField lastNameField = new TextField("Last Name");
    private final EmailField emailField = new EmailField("Email");
    private final IntegerField weeklyTargetHoursField = new IntegerField("Weekly Target Hours");
    private final Button saveProfileButton = new Button("Save Changes");

    // Password Section
    private final PasswordField currentPasswordField = new PasswordField("Current Password");
    private final PasswordField newPasswordField = new PasswordField("New Password");
    private final PasswordField confirmPasswordField = new PasswordField("Confirm New Password");
    private final Button changePasswordButton = new Button("Change Password");

    private final Binder<PasswordChangeForm> passwordBinder = new Binder<>(PasswordChangeForm.class);

    public SettingsView(UserService userService) {
        this.userService = userService;

        addClassName("settings-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Get current user
        this.currentUser = Optional.ofNullable(userService.getCurrentUser())
                .orElseThrow(() -> new IllegalStateException("No user logged in"));

        // Page Title
        H2 pageTitle = new H2("Settings");
        pageTitle.getStyle()
                .set("margin", "0 0 2rem 0")
                .set("color", "hsl(38, 95%, 65%)")
                .set("font-weight", "700");

        // Main Container
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setMaxWidth("800px");
        mainContainer.setPadding(false);
        mainContainer.setSpacing(true);
        mainContainer.getStyle().set("gap", "2rem");

        // Profile Section
        VerticalLayout profileSection = createProfileSection();

        // Password Section
        VerticalLayout passwordSection = createPasswordSection();

        // Account Section
        VerticalLayout accountSection = createAccountSection();

        mainContainer.add(profileSection, passwordSection, accountSection);

        add(pageTitle, mainContainer);
    }

    private VerticalLayout createProfileSection() {
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

        H3 sectionTitle = new H3("Profile Information");
        sectionTitle.getStyle()
                .set("margin", "0 0 1rem 0")
                .set("color", "var(--lumo-body-text-color)")
                .set("font-size", "20px");

        // Load current user data
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

        saveProfileButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveProfileButton.addClickListener(e -> saveProfile());

        section.add(sectionTitle, firstNameField, lastNameField, emailField,
                weeklyTargetHoursField, saveProfileButton);
        return section;
    }

    private VerticalLayout createPasswordSection() {
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

        H3 sectionTitle = new H3("Change Password");
        sectionTitle.getStyle()
                .set("margin", "0 0 1rem 0")
                .set("color", "var(--lumo-body-text-color)")
                .set("font-size", "20px");

        currentPasswordField.setWidthFull();
        currentPasswordField.setRequired(true);

        newPasswordField.setWidthFull();
        newPasswordField.setRequired(true);
        newPasswordField.setHelperText("At least 8 characters");

        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequired(true);

        // Validation
        setupPasswordValidation();

        changePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changePasswordButton.addClickListener(e -> changePassword());

        section.add(
                sectionTitle,
                currentPasswordField,
                newPasswordField,
                confirmPasswordField,
                changePasswordButton
        );
        return section;
    }

    private VerticalLayout createAccountSection() {
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

        H3 sectionTitle = new H3("Account Information");
        sectionTitle.getStyle()
                .set("margin", "0 0 1rem 0")
                .set("color", "var(--lumo-body-text-color)")
                .set("font-size", "20px");

        // Account Stats
        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setWidthFull();
        statsLayout.setSpacing(true);

        VerticalLayout createdStat = createStatBox(
                "Member Since",
                currentUser.getCreatedAt() != null
                        ? currentUser.getCreatedAt().toLocalDate().toString()
                        : "Unknown"
        );

        VerticalLayout lastLoginStat = createStatBox(
                "Last Login",
                currentUser.getLastLoginAt() != null
                        ? currentUser.getLastLoginAt().toLocalDate().toString()
                        : "Never"
        );

        statsLayout.add(createdStat, lastLoginStat);

        section.add(sectionTitle, statsLayout);
        return section;
    }

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

    private void setupPasswordValidation() {
        passwordBinder.forField(currentPasswordField)
                .asRequired("Current password is required")
                .bind(PasswordChangeForm::getCurrentPassword, PasswordChangeForm::setCurrentPassword);

        passwordBinder.forField(newPasswordField)
                .asRequired("New password is required")
                .withValidator(password -> password.length() >= 8,
                        "Password must be at least 8 characters")
                .bind(PasswordChangeForm::getNewPassword, PasswordChangeForm::setNewPassword);

        passwordBinder.forField(confirmPasswordField)
                .asRequired("Please confirm your new password")
                .withValidator(confirmPassword ->
                                confirmPassword.equals(newPasswordField.getValue()),
                        "Passwords do not match")
                .bind(PasswordChangeForm::getConfirmPassword, PasswordChangeForm::setConfirmPassword);
    }

    private void saveProfile() {
        Integer weeklyHours = weeklyTargetHoursField.getValue();
        if (weeklyHours == null || weeklyHours < 0 || weeklyHours > 80) {
            Notification.show(
                    "Weekly target hours must be between 0 and 80.",
                    4000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            userService.updateProfile(
                    currentUser.getEmail(),
                    firstNameField.getValue(),
                    lastNameField.getValue(),
                    weeklyHours
            );

            // Update local user object
            currentUser.setFirstName(firstNameField.getValue());
            currentUser.setLastName(lastNameField.getValue());
            currentUser.setWeeklyTargetHours(weeklyHours);

            Notification.show(
                    "Profile updated successfully!",
                    3000,
                    Notification.Position.TOP_CENTER
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (Exception e) {
            log.error("Failed to update profile for user", e);
            Notification.show(
                    "Failed to update profile. Please try again.",
                    5000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void changePassword() {
        PasswordChangeForm form = new PasswordChangeForm();

        if (passwordBinder.writeBeanIfValid(form)) {
            try {
                userService.changePassword(
                        currentUser.getEmail(),
                        form.getCurrentPassword(),
                        form.getNewPassword()
                );

                Notification.show(
                        "Password changed successfully!",
                        3000,
                        Notification.Position.TOP_CENTER
                ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // Clear fields
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();

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

    // Helper class for password change form
    @Data
    public static class PasswordChangeForm {
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;
    }
}