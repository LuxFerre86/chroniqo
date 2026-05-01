package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.frontend.dashboard.QuickStatsWidget;
import com.luxferre.chroniqo.frontend.dashboard.TodaySummaryCard;
import com.luxferre.chroniqo.frontend.dashboard.WeekChartWidget;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.timepicker.TimePicker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@WithMockUser("test@gmail.com")
@ViewPackages(classes = DashboardView.class)
class DashboardViewTest extends SpringBrowserlessTest {

    @Nested
    @DisplayName("Layout & Structure")
    @WithMockUser("test@gmail.com")
    class LayoutAndStructure {

        @Test
        void onLoad_dashboardTitleIsVisible() {
            navigate(DashboardView.class);
            assertThat($(H2.class).withText("Dashboard").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_headerContainsTodaysDate() {
            navigate(DashboardView.class);
            String expectedDate = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.UK));
            assertThat($(Span.class).withText(expectedDate).single().isVisible()).isTrue();
        }

        @Test
        void onLoad_todaySummaryCardIsPresent() {
            navigate(DashboardView.class);
            assertThat($(TodaySummaryCard.class).all()).isNotEmpty();
        }

        @Test
        void onLoad_weekChartWidgetIsPresent() {
            navigate(DashboardView.class);
            assertThat($(WeekChartWidget.class).all()).isNotEmpty();
        }

        @Test
        void onLoad_quickStatsWidgetIsPresent() {
            navigate(DashboardView.class);
            assertThat($(QuickStatsWidget.class).all()).isNotEmpty();
        }

        @Test
        void onLoad_currentBalanceIsReturned() {
            DashboardView view = navigate(DashboardView.class);
            assertThatCode(view::getCurrentBalance).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Quick Actions")
    @WithMockUser("test@gmail.com")
    class QuickActions {

        @Test
        void onLoad_logTimeButtonIsUsable() {
            navigate(DashboardView.class);
            assertThat(test($(Button.class).withText("Log Time").single()).isUsable()).isTrue();
        }

        @Test
        void onLoad_viewMonthButtonIsUsable() {
            navigate(DashboardView.class);
            assertThat(test($(Button.class).withText("View Month").single()).isUsable()).isTrue();
        }

        @Test
        void onLoad_refreshButtonIsUsable() {
            navigate(DashboardView.class);
            assertThat(test($(Button.class).withText("Refresh").single()).isUsable()).isTrue();
        }

        @Test
        void clickRefreshButton_doesNotThrow() {
            navigate(DashboardView.class);
            Button refreshButton = $(Button.class).withText("Refresh").single();
            assertThatCode(() -> test(refreshButton).click()).doesNotThrowAnyException();
        }

        @Test
        void clickRefreshButton_calledTwice_doesNotThrow() {
            navigate(DashboardView.class);
            Button refreshButton = $(Button.class).withText("Refresh").single();
            assertThatCode(() -> {
                test(refreshButton).click();
                test(refreshButton).click();
            }).doesNotThrowAnyException();
        }

        @Test
        void clickViewMonthButton_navigatesToMonthView() {
            navigate(DashboardView.class);
            test($(Button.class).withText("View Month").single()).click();
            assertThat(getCurrentView()).isInstanceOf(MonthView.class);
        }

        @Test
        void clickLogTimeButton_opensDialog() {
            navigate(DashboardView.class);
            test($(Button.class).withText("Log Time").single()).click();
            assertThat($(Dialog.class).single().isOpened()).isTrue();
        }
    }

    @Nested
    @DisplayName("Time Entry Dialog")
    @WithMockUser("test@gmail.com")
    class TimeEntryDialogTests {

        private Dialog openDialog() {
            navigate(DashboardView.class);
            test($(Button.class).withText("Log Time").single()).click();
            return $(Dialog.class).single();
        }

        @Test
        void onOpen_datePickerIsPresetToToday() {
            Dialog dialog = openDialog();
            DatePicker datePicker = $(DatePicker.class, dialog).withValue(LocalDate.now()).single();
            assertThat(datePicker.getValue()).isEqualTo(LocalDate.now());
        }

        @Test
        void onOpen_workingTimeTabIsSelectedByDefault() {
            Dialog dialog = openDialog();
            assertThat(test($(Tabs.class, dialog).single()).isSelected("Working Time")).isTrue();
        }

        @Test
        void onOpen_allThreeTabsArePresent() {
            Dialog dialog = openDialog();
            Tabs tabs = $(Tabs.class, dialog).single();
            assertThat(test(tabs).getTab("Working Time")).isNotNull();
            assertThat(test(tabs).getTab("Vacation")).isNotNull();
            assertThat(test(tabs).getTab("Sick")).isNotNull();
        }

        @Test
        void onOpen_workingTimeMode_timePickersAreVisible() {
            Dialog dialog = openDialog();
            assertThat($(TimePicker.class, dialog).all())
                    .isNotEmpty()
                    .allSatisfy(tp -> assertThat(tp.isVisible()).isTrue());
        }

        @Test
        void onOpen_noExistingEntry_deleteButtonIsNotVisible() {
            Dialog dialog = openDialog();
            // withText() only matches visible components — empty result means the button is hidden
            assertThat($(Button.class, dialog).withText("Delete").all()).isEmpty();
        }

        @Test
        void onOpen_saveButtonIsUsable() {
            Dialog dialog = openDialog();
            assertThat(test($(Button.class, dialog).withText("Save").single()).isUsable()).isTrue();
        }

        @Test
        void switchToSickTab_saveButtonTextChangesToMarkAsSick() {
            Dialog dialog = openDialog();
            test($(Tabs.class, dialog).single()).select("Sick");
            assertThat($(Button.class, dialog).withText("Mark as Sick").all()).isNotEmpty();
        }

        @Test
        void switchToVacationTab_saveButtonTextChangesToBookVacation() {
            Dialog dialog = openDialog();
            test($(Tabs.class, dialog).single()).select("Vacation");
            assertThat($(Button.class, dialog).withText("Book Vacation").all()).isNotEmpty();
        }

        @Test
        void switchToSickTab_timePickersAreHidden() {
            Dialog dialog = openDialog();
            test($(Tabs.class, dialog).single()).select("Sick");
            assertThat($(TimePicker.class, dialog).all())
                    .allSatisfy(tp -> assertThat(tp.isVisible()).isFalse());
        }
    }

    @Nested
    @DisplayName("Time Entry Dialog – Validation Messages")
    @WithMockUser("test@gmail.com")
    class TimeEntryDialogValidationMessages {

        private Dialog openDialog() {
            navigate(DashboardView.class);
            test($(Button.class).withText("Log Time").single()).click();
            return $(Dialog.class).single();
        }

        @Test
        void clickSaveWithEndTimeBeforeStartTime_showsInvalidRangeError() {
            Dialog dialog = openDialog();
            test($(TimePicker.class, dialog).withCondition(tp -> "Start Time".equals(tp.getLabel())).single())
                    .setValue(LocalTime.of(10, 0));
            test($(TimePicker.class, dialog).withCondition(tp -> "End Time".equals(tp.getLabel())).single())
                    .setValue(LocalTime.of(9, 0));
            test($(Button.class, dialog).withText("Save").single()).click();

            assertThat($(Div.class, dialog)
                    .withCondition(d -> d.getText() != null && d.getText().contains("End time must be after start time."))
                    .all())
                    .isNotEmpty();
        }

        @Test
        void clickSaveWithEndTimeBeforeStartTime_dialogRemainsOpen() {
            Dialog dialog = openDialog();
            test($(TimePicker.class, dialog).withCondition(tp -> "Start Time".equals(tp.getLabel())).single())
                    .setValue(LocalTime.of(10, 0));
            test($(TimePicker.class, dialog).withCondition(tp -> "End Time".equals(tp.getLabel())).single())
                    .setValue(LocalTime.of(9, 0));
            test($(Button.class, dialog).withText("Save").single()).click();

            assertThat(dialog.isOpened()).isTrue();
        }

        @Test
        void clickSaveWithEqualStartAndEndTime_showsSameTimeError() {
            Dialog dialog = openDialog();
            test($(TimePicker.class, dialog).withCondition(tp -> "Start Time".equals(tp.getLabel())).single())
                    .setValue(LocalTime.of(10, 0));
            test($(TimePicker.class, dialog).withCondition(tp -> "End Time".equals(tp.getLabel())).single())
                    .setValue(LocalTime.of(10, 0));
            test($(Button.class, dialog).withText("Save").single()).click();

            assertThat($(Div.class, dialog)
                    .withCondition(d -> d.getText() != null && d.getText().contains("End time cannot be the same as start time."))
                    .all())
                    .isNotEmpty();
        }

        @Test
        void clickSaveWhenBreakExceedsTotalDuration_showsInconsistentDataError() {
            Dialog dialog = openDialog();
            test($(TimePicker.class, dialog).withCondition(tp -> "Start Time".equals(tp.getLabel())).single())
                    .setValue(LocalTime.of(9, 0));
            test($(TimePicker.class, dialog).withCondition(tp -> "End Time".equals(tp.getLabel())).single())
                    .setValue(LocalTime.of(10, 0));
            // total span = 60 min; set break to 90 min → break exceeds total
            test($(IntegerField.class, dialog).withCondition(f -> "Break".equals(f.getLabel())).single())
                    .setValue(90);
            test($(Button.class, dialog).withText("Save").single()).click();

            assertThat($(Div.class, dialog)
                    .withCondition(d -> d.getText() != null && d.getText().contains("Break time cannot be longer than total working time."))
                    .all())
                    .isNotEmpty();
        }

        @Test
        void switchingTabAfterError_clearsErrorMessage() {
            // Trigger a validation error: end before start
            Dialog dialog = openDialog();
            test($(TimePicker.class, dialog).withCondition(tp -> "Start Time".equals(tp.getLabel())).single())
                    .setValue(LocalTime.of(10, 0));
            test($(TimePicker.class, dialog).withCondition(tp -> "End Time".equals(tp.getLabel())).single())
                    .setValue(LocalTime.of(9, 0));
            test($(Button.class, dialog).withText("Save").single()).click();
            // Dialog stays open with error — switch tab to clear it
            test($(Tabs.class, dialog).single()).select("Sick");

            assertThat($(Div.class, dialog)
                    .withCondition(d -> d.getText() != null && d.getText().contains("End time must be after start time."))
                    .all())
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Access Control")
    class AccessControl {

        @Test
        @WithAnonymousUser
        void anonymousUser_redirectsToLogin() {
            navigate("", LoginView.class);
        }
    }
}
