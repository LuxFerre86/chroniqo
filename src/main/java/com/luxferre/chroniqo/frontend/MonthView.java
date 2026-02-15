package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.service.MonthService;
import com.luxferre.chroniqo.service.YearService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Route("month")
@UIScope
@Component
@StyleSheet("calendar.css")
public class MonthView extends VerticalLayout {
    private YearMonth currentMonth = YearMonth.now();
    private final Div calendarGrid;
    private final MonthService monthService;
    private final YearService yearService;
    private Map<LocalDate, DaySummaryDTO> monthSummaries = new HashMap<>();
    private Map<LocalDate, DaySummaryDTO> yearSummaries = new HashMap<>();
    private final TimeEntryDialog timeEntryDialog;
    private final StatisticsCard monthStatisticsCard;

    public MonthView(MonthService monthService, YearService yearService, TimeEntryDialog timeEntryDialog) {
        this.monthService = monthService;
        this.yearService = yearService;
        this.timeEntryDialog = timeEntryDialog;
        this.timeEntryDialog.addClosedListener(event1 -> {
            loadSummaries();
            renderCalendar();
            updateStatistics();
        });

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        MonthSelector monthSelector = new MonthSelector();
        monthSelector.setChangeListener(newMonth -> {
            currentMonth = newMonth;
            loadSummaries();
            renderCalendar();
            updateStatistics();
        });
        monthSelector.setSelectedMonth(currentMonth);
        monthSelector.setAlignSelf(Alignment.CENTER);

        monthStatisticsCard = new StatisticsCard();

        calendarGrid = new Div();
        calendarGrid.addClassName("calendar-grid");
        calendarGrid.setWidthFull();

        loadSummaries();
        renderCalendar();
        updateStatistics();

        add(monthSelector, monthStatisticsCard, calendarGrid);
        setHorizontalComponentAlignment(Alignment.CENTER, monthSelector);
        setFlexGrow(1, calendarGrid);
    }

    private void renderCalendar() {
        calendarGrid.removeAll();

        // ===== Header =====
        DayOfWeek[] weekdays = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };

        for (DayOfWeek day : weekdays) {
            Div header = new Div();
            header.setText(day.getDisplayName(TextStyle.SHORT, UI.getCurrent().getLocale()));
            header.addClassName("calendar-header");

            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                header.addClassName("weekend-header");
            }

            calendarGrid.add(header);
        }

        // ===== Tage =====
        LocalDate firstDay = currentMonth.atDay(1);
        int firstWeekdayIndex = firstDay.getDayOfWeek().getValue() - 1;

        // Leere Zellen vor Monatsbeginn
        for (int i = 0; i < firstWeekdayIndex; i++) {
            Div emptyCell = new Div();
            emptyCell.addClassName("calendar-empty");
            calendarGrid.add(emptyCell);
        }

        // Monatstage
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);
            calendarGrid.add(createDayCard(date));
        }
    }

    private Div createDayCard(LocalDate date) {
        DaySummaryDTO summary = monthSummaries.get(date);
        boolean isToday = date.equals(LocalDate.now());
        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;

        Div dayCard = new Div();
        dayCard.addClassName("day-card");

        if (isToday) {
            dayCard.addClassName("day-card--today");
        }

        if (isWeekend) {
            dayCard.addClassName("day-card--weekend");
        }

        // ===== Day Header Layout =====
        HorizontalLayout dayHeader = new HorizontalLayout();
        dayHeader.setWidthFull();
        dayHeader.setJustifyContentMode(JustifyContentMode.BETWEEN);
        dayHeader.setAlignItems(Alignment.CENTER);
        dayHeader.addClassName("day-header");
        dayHeader.setPadding(false);
        dayHeader.setSpacing(false);

        // Day Number + Weekday (Mobile)
        VerticalLayout dayNumberSection = new VerticalLayout();
        dayNumberSection.setPadding(false);
        dayNumberSection.setSpacing(false);
        dayNumberSection.addClassName("day-number-section");

        Span weekdayLabel = new Span(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, UI.getCurrent().getLocale()));
        weekdayLabel.addClassName("weekday-mobile");

        Span dayNumber = new Span(String.valueOf(date.getDayOfMonth()));
        dayNumber.addClassName("day-number");

        dayNumberSection.add(weekdayLabel, dayNumber);

        // Badge
        Div badgeContainer = new Div();
        String dayTypeLabel = getDayTypeLabel(summary.absenceType());
        if (StringUtils.hasText(dayTypeLabel)) {
            Span badge = new Span(dayTypeLabel);
            badge.addClassName("day-badge");
            badge.addClassName(getBadgeClassName(summary.absenceType()));
            badgeContainer.add(badge);
        }

        dayHeader.add(dayNumberSection, badgeContainer);

        // ===== Day Content =====
        VerticalLayout dayContent = new VerticalLayout();
        dayContent.setWidthFull();
        dayContent.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        dayContent.setJustifyContentMode(JustifyContentMode.CENTER);
        dayContent.addClassName("day-content");
        dayContent.setPadding(false);
        dayContent.setSpacing(false);

        // Worked Hours
        Span workedHours = new Span(formatMinutes(summary.workedMinutes()));
        workedHours.addClassName("worked-hours");

        // Balance
        Span balance = new Span(formatBalance(summary.balanceMinutes()));
        balance.addClassName("balance");
        if (summary.balanceMinutes() != null) {
            balance.addClassName(summary.balanceMinutes() >= 0 ? "balance-positive" : "balance-negative");
        }

        dayContent.add(workedHours, balance);

        dayCard.add(dayHeader, dayContent);

        // Click Handler
        dayCard.getElement().addEventListener("click", event -> timeEntryDialog.open(date));

        return dayCard;
    }

    private String getBadgeClassName(AbsenceType type) {
        if (type == null) return "";
        return switch (type) {
            case HOLIDAY -> "badge-holiday";
            case VACATION -> "badge-vacation";
            case SICK -> "badge-sick";
        };
    }

    private void updateStatistics() {
        updateMonthStatistics();
        updateYearStatistics();
    }

    private void updateMonthStatistics() {
        int totalTargetMinutes = 0;
        int totalWorkedMinutes = 0;

        for (DaySummaryDTO summary : monthSummaries.values()) {
            if (summary.date().isBefore(LocalDate.now())) {
                if (summary.absenceType() == null &&
                        summary.date().getDayOfWeek() != DayOfWeek.SATURDAY &&
                        summary.date().getDayOfWeek() != DayOfWeek.SUNDAY) {
                    totalTargetMinutes += 468;
                }

                if (summary.workedMinutes() != null) {
                    totalWorkedMinutes += summary.workedMinutes();
                }
            }
        }

        monthStatisticsCard.updateMonthStatistics(totalTargetMinutes, totalWorkedMinutes);
    }

    private void updateYearStatistics() {
        int totalTargetMinutes = 0;
        int totalWorkedMinutes = 0;

        for (DaySummaryDTO summary : yearSummaries.values()) {
            if (summary.date().isBefore(LocalDate.now())) {
                if (summary.absenceType() == null &&
                        summary.date().getDayOfWeek() != DayOfWeek.SATURDAY &&
                        summary.date().getDayOfWeek() != DayOfWeek.SUNDAY) {
                    totalTargetMinutes += 468;
                }

                if (summary.workedMinutes() != null) {
                    totalWorkedMinutes += summary.workedMinutes();
                }
            }
        }

        monthStatisticsCard.updateYearStatistics(totalTargetMinutes, totalWorkedMinutes);
    }

    private String formatMinutes(Integer minutes) {
        if (minutes == null) return "";

        int sign = minutes < 0 ? -1 : 1;
        int absMinutes = Math.abs(minutes);

        int hours = absMinutes / 60;
        int mins = absMinutes % 60;

        return String.format("%s%d:%02d h", sign < 0 ? "-" : "", hours, mins);
    }

    private String formatBalance(Integer minutes) {
        if (minutes == null) return "";
        String sign = minutes >= 0 ? "+" : "";
        return sign + formatMinutes(minutes);
    }

    private String getDayTypeLabel(AbsenceType type) {
        if (null == type) {
            return null;
        } else {
            return switch (type) {
                case HOLIDAY -> "Feiertag";
                case VACATION -> "Urlaub";
                case SICK -> "Krank";
            };
        }
    }

    private void loadSummaries() {
        monthSummaries = monthService.getMonth(currentMonth.getYear(), currentMonth.getMonthValue())
                .stream()
                .collect(Collectors.toMap(DaySummaryDTO::date, dto -> dto));
        yearSummaries = yearService.getYear(currentMonth.getYear())
                .stream()
                .collect(Collectors.toMap(DaySummaryDTO::date, dto -> dto));
    }
}