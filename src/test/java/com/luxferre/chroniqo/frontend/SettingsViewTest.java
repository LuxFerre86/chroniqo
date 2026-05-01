package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.CountrySubdivisionRegistry;
import com.luxferre.chroniqo.service.user.UserService;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ViewPackages(classes = SettingsView.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SettingsViewTest extends SpringBrowserlessTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CountrySubdivisionRegistry countryRegistry;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setEmail("test@gmail.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(countryRegistry.getAllCountries()).thenReturn(Collections.emptyMap());
        when(countryRegistry.getSubdivisions(any())).thenReturn(Collections.emptyMap());
    }

    @Nested
    @DisplayName("Layout & Structure")
    @WithMockUser("test@gmail.com")
    class LayoutAndStructure {

        @Test
        void onLoad_showsSettingsTitle() {
            navigate(SettingsView.class);
            assertThat($(H2.class).withText("Settings").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_showsProfileSectionHeading() {
            navigate(SettingsView.class);
            assertThat($(H3.class).withText("Profile Information").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_showsChangePasswordSectionHeading() {
            navigate(SettingsView.class);
            assertThat($(H3.class).withText("Change Password").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_showsAccountInformationSectionHeading() {
            navigate(SettingsView.class);
            assertThat($(H3.class).withText("Account Information").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_showsDangerZoneSectionHeading() {
            navigate(SettingsView.class);
            assertThat($(H3.class).withText("Danger Zone").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_saveChangesButtonIsUsable() {
            navigate(SettingsView.class);
            assertThat(test($(Button.class).withText("Save Changes").single()).isUsable()).isTrue();
        }

        @Test
        void onLoad_changePasswordButtonIsUsable() {
            navigate(SettingsView.class);
            assertThat(test($(Button.class).withText("Change Password").single()).isUsable()).isTrue();
        }

        @Test
        void onLoad_deleteAccountButtonIsUsable() {
            navigate(SettingsView.class);
            assertThat(test($(Button.class).withText("Delete Account").single()).isUsable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Profile Section")
    @WithMockUser("test@gmail.com")
    class ProfileSection {

        @Test
        void saveWithValidData_showsSuccessNotification() {
            navigate(SettingsView.class);
            test($(Button.class).withText("Save Changes").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("Profile updated successfully!");
        }
    }

    @Nested
    @DisplayName("Password Section – Validation Messages")
    @WithMockUser("test@gmail.com")
    class PasswordSectionValidationMessages {

        @Test
        void emptyPasswordForm_showsValidationErrorNotification() {
            navigate(SettingsView.class);
            test($(Button.class).withText("Change Password").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("Please fix the errors in the form");
        }

        @Test
        void emptyCurrentPassword_marksCurrentPasswordFieldAsInvalid() {
            navigate(SettingsView.class);
            test($(Button.class).withText("Change Password").single()).click();

            assertThat($(PasswordField.class).withCondition(f -> "Current Password".equals(f.getLabel())).single().isInvalid()).isTrue();
        }

        @Test
        void emptyCurrentPassword_showsRequiredErrorOnCurrentPasswordField() {
            navigate(SettingsView.class);
            test($(Button.class).withText("Change Password").single()).click();

            assertThat($(PasswordField.class).withCondition(f -> "Current Password".equals(f.getLabel())).single().getErrorMessage())
                    .contains("Current password is required");
        }

        @Test
        void emptyNewPassword_marksNewPasswordFieldAsInvalid() {
            navigate(SettingsView.class);
            test($(Button.class).withText("Change Password").single()).click();

            assertThat($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single().isInvalid()).isTrue();
        }

        @Test
        void emptyNewPassword_showsRequiredErrorOnNewPasswordField() {
            navigate(SettingsView.class);
            test($(Button.class).withText("Change Password").single()).click();

            assertThat($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single().getErrorMessage())
                    .contains("New password is required");
        }

        @Test
        void mismatchingPasswords_marksConfirmPasswordFieldAsInvalid() {
            navigate(SettingsView.class);
            test($(PasswordField.class).withCondition(f -> "Current Password".equals(f.getLabel())).single()).setValue("currentPassword1!");
            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single()).setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm New Password".equals(f.getLabel())).single()).setValue("DifferentPass1!");
            test($(Button.class).withText("Change Password").single()).click();

            assertThat($(PasswordField.class).withCondition(f -> "Confirm New Password".equals(f.getLabel())).single().isInvalid()).isTrue();
        }

        @Test
        void mismatchingPasswords_showsMismatchErrorOnConfirmPasswordField() {
            navigate(SettingsView.class);
            test($(PasswordField.class).withCondition(f -> "Current Password".equals(f.getLabel())).single()).setValue("currentPassword1!");
            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single()).setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm New Password".equals(f.getLabel())).single()).setValue("DifferentPass1!");
            test($(Button.class).withText("Change Password").single()).click();

            assertThat($(PasswordField.class).withCondition(f -> "Confirm New Password".equals(f.getLabel())).single().getErrorMessage())
                    .contains("Passwords do not match");
        }

        @Test
        void validPasswordChange_showsSuccessNotification() {
            navigate(SettingsView.class);
            test($(PasswordField.class).withCondition(f -> "Current Password".equals(f.getLabel())).single()).setValue("currentPassword1!");
            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single()).setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm New Password".equals(f.getLabel())).single()).setValue("SecurePass1!");
            test($(Button.class).withText("Change Password").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("Password changed successfully!");
        }

        @Test
        void wrongCurrentPassword_showsErrorNotification() {
            doThrow(new IllegalArgumentException("Current password is incorrect.")).when(userService).changePassword(any(), any(), any());
            navigate(SettingsView.class);
            test($(PasswordField.class).withCondition(f -> "Current Password".equals(f.getLabel())).single()).setValue("wrongPassword1!");
            test($(PasswordField.class).withCondition(f -> "New Password".equals(f.getLabel())).single()).setValue("SecurePass1!");
            test($(PasswordField.class).withCondition(f -> "Confirm New Password".equals(f.getLabel())).single()).setValue("SecurePass1!");
            test($(Button.class).withText("Change Password").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("Current password is incorrect.");
        }
    }

    @Nested
    @DisplayName("Delete Account Dialog")
    @WithMockUser("test@gmail.com")
    class DeleteAccountDialog {

        private Dialog openDeleteDialog() {
            navigate(SettingsView.class);
            test($(Button.class).withText("Delete Account").single()).click();
            return $(Dialog.class).single();
        }

        @Test
        void clickDeleteAccount_opensDialog() {
            navigate(SettingsView.class);
            test($(Button.class).withText("Delete Account").single()).click();

            assertThat($(Dialog.class).single().isOpened()).isTrue();
        }

        @Test
        void clickCancel_closesDialog() {
            Dialog dialog = openDeleteDialog();
            test($(Button.class, dialog).withText("Cancel").single()).click();

            assertThat(dialog.isOpened()).isFalse();
        }

        @Test
        void withoutDeleteConfirmation_showsErrorNotification() {
            Dialog dialog = openDeleteDialog();
            test($(PasswordField.class, dialog).withCondition(f -> "Current Password".equals(f.getLabel())).single())
                    .setValue("somePassword1!");
            test($(Button.class, dialog).withText("Delete Account").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("Please type DELETE to confirm account removal.");
        }

        @Test
        void withoutPassword_showsErrorNotification() {
            Dialog dialog = openDeleteDialog();
            test($(TextField.class, dialog).withCondition(f -> "Confirmation".equals(f.getLabel())).single())
                    .setValue("DELETE");
            test($(Button.class, dialog).withText("Delete Account").single()).click();

            assertThat(test($(Notification.class).single()).getText())
                    .contains("Please enter your current password.");
        }
    }

    @Nested
    @DisplayName("Access Control")
    class AccessControl {

        @Test
        @WithAnonymousUser
        void anonymousUser_redirectsToLogin() {
            navigate("settings", LoginView.class);
        }
    }
}
