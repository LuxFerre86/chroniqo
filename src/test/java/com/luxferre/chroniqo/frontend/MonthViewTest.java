package com.luxferre.chroniqo.frontend;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@WithMockUser("test@gmail.com")
@ViewPackages(classes = MonthView.class)
class MonthViewTest extends SpringBrowserlessTest {

    private String formatYearMonth(YearMonth yearMonth) {
        return yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.UK) + " " + yearMonth.getYear();
    }

    /** Returns the previous/next icon buttons from MonthSelector, excluding the "Today" button. */
    private List<Button> navButtons() {
        MonthSelector monthSelector = $(MonthSelector.class).single();
        return $(Button.class, monthSelector).all().stream()
                .filter(b -> !"Today".equals(b.getText()))
                .toList();
    }

    @Nested
    @DisplayName("Layout & Structure")
    class LayoutAndStructure {

        @Test
        void onLoad_monthSelectorIsPresent() {
            navigate(MonthView.class);
            assertThat($(MonthSelector.class).all()).isNotEmpty();
        }

        @Test
        void onLoad_statisticsCardIsPresent() {
            navigate(MonthView.class);
            assertThat($(StatisticsCard.class).all()).isNotEmpty();
        }

        @Test
        void onLoad_calendarGridIsPresent() {
            navigate(MonthView.class);
            assertThat($(Div.class).withCondition(div -> div.hasClassName("calendar-grid")).all()).isNotEmpty();
        }

        @Test
        void onLoad_targetHoursLabelIsVisible() {
            navigate(MonthView.class);
            assertThat($(Span.class).withText("Target Hours").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_workedHoursLabelIsVisible() {
            navigate(MonthView.class);
            assertThat($(Span.class).withText("Worked Hours").single().isVisible()).isTrue();
        }

        @Test
        void onLoad_balanceLabelIsVisible() {
            navigate(MonthView.class);
            assertThat($(Span.class).withText("Balance").single().isVisible()).isTrue();
        }
    }

    @Nested
    @DisplayName("Calendar Structure")
    class CalendarStructure {

        @Test
        void onLoad_sevenDayHeadersArePresent() {
            navigate(MonthView.class);
            List<Div> headers = $(Div.class).withCondition(div -> div.hasClassName("calendar-header")).all();
            assertThat(headers).hasSize(7);
        }

        @Test
        void onLoad_firstDayHeaderIsMonday() {
            navigate(MonthView.class);
            List<Div> headers = $(Div.class).withCondition(div -> div.hasClassName("calendar-header")).all();
            assertThat(headers.getFirst().getText()).isEqualTo("Mon");
        }

        @Test
        void onLoad_lastDayHeaderIsSunday() {
            navigate(MonthView.class);
            List<Div> headers = $(Div.class).withCondition(div -> div.hasClassName("calendar-header")).all();
            assertThat(headers.get(6).getText()).isEqualTo("Sun");
        }

        @Test
        void onLoad_dayCardCountMatchesDaysInCurrentMonth() {
            navigate(MonthView.class);
            int expectedDays = YearMonth.now().lengthOfMonth();
            // "day-card--today" also carries "day-card", so all day cards are matched
            List<Div> dayCards = $(Div.class).withCondition(div -> div.hasClassName("day-card")).all();
            assertThat(dayCards).hasSize(expectedDays);
        }

        @Test
        void onLoad_exactlyOneTodayCardIsHighlighted() {
            navigate(MonthView.class);
            List<Div> todayCards = $(Div.class).withCondition(div -> div.hasClassName("day-card--today")).all();
            assertThat(todayCards).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Month Navigation")
    class MonthNavigation {

        @Test
        void onLoad_currentMonthLabelIsVisible() {
            navigate(MonthView.class);
            assertThat($(Span.class).withText(formatYearMonth(YearMonth.now())).single().isVisible()).isTrue();
        }

        @Test
        void onLoad_todayButtonIsCssHiddenOnCurrentMonth() {
            navigate(MonthView.class);
            // CSS visibility:hidden (not setVisible) keeps the element in flow — isVisible() returns true
            Button todayButton = $(Button.class).withText("Today").single();
            assertThat(todayButton.getStyle().get("visibility")).isEqualTo("hidden");
        }

        @Test
        void clickPreviousButton_showsPreviousMonthLabel() {
            navigate(MonthView.class);
            test(navButtons().getFirst()).click();
            YearMonth prev = YearMonth.now().minusMonths(1);
            assertThat($(Span.class).withText(formatYearMonth(prev)).single().isVisible()).isTrue();
        }

        @Test
        void clickNextButton_showsNextMonthLabel() {
            navigate(MonthView.class);
            test(navButtons().get(1)).click();
            YearMonth next = YearMonth.now().plusMonths(1);
            assertThat($(Span.class).withText(formatYearMonth(next)).single().isVisible()).isTrue();
        }

        @Test
        void clickPreviousButton_dayCardCountMatchesDaysInPreviousMonth() {
            navigate(MonthView.class);
            test(navButtons().getFirst()).click();
            int expectedDays = YearMonth.now().minusMonths(1).lengthOfMonth();
            List<Div> dayCards = $(Div.class).withCondition(div -> div.hasClassName("day-card")).all();
            assertThat(dayCards).hasSize(expectedDays);
        }

        @Test
        void clickPreviousButton_todayButtonBecomesVisible() {
            navigate(MonthView.class);
            test(navButtons().getFirst()).click();
            Button todayButton = $(Button.class).withText("Today").single();
            assertThat(todayButton.getStyle().get("visibility")).isEqualTo("visible");
        }

        @Test
        void clickTodayButton_afterNavigation_returnsToCurrentMonth() {
            navigate(MonthView.class);
            test(navButtons().getFirst()).click();
            test($(Button.class).withText("Today").single()).click();
            assertThat($(Span.class).withText(formatYearMonth(YearMonth.now())).single().isVisible()).isTrue();
        }

        @Test
        void clickTodayButton_afterNavigation_todayButtonBecomesHiddenAgain() {
            navigate(MonthView.class);
            test(navButtons().getFirst()).click();
            test($(Button.class).withText("Today").single()).click();
            assertThat($(Button.class).withText("Today").single().getStyle().get("visibility")).isEqualTo("hidden");
        }
    }

    @Nested
    @DisplayName("Access Control")
    class AccessControl {

        @Test
        @WithAnonymousUser
        void anonymousUser_redirectsToLogin() {
            navigate("month", LoginView.class);
        }
    }
}