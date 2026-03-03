package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.service.SummaryService;
import com.luxferre.chroniqo.util.IsWeekendQuery;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Route("month")
@UIScope
@Component
@StyleSheet("calendar.css")
@RolesAllowed("ROLE_USER")
public class MonthView extends VerticalLayout {
    private YearMonth currentMonth = YearMonth.now();
    private final Div calendarGrid;
    private final SummaryService summaryService;
    private Map<LocalDate, DaySummaryDTO> monthSummaries = new HashMap<>();
    private Map<LocalDate, DaySummaryDTO> yearSummaries = new HashMap<>();
    private final TimeEntryDialog timeEntryDialog;
    private final StatisticsCard monthStatisticsCard;

    public MonthView(SummaryService summaryService, TimeEntryDialog timeEntryDialog) {
        this.summaryService = summaryService;
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
            header.setText(day.getDisplayName(TextStyle.SHORT, Locale.UK));
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
        boolean isWeekend = date.query(new IsWeekendQuery());
        boolean isAbsence = summary.absenceType() != null;

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

        Span weekdayLabel = new Span(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.UK));
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
        Span workedHours = new Span();
        workedHours.addClassName("worked-hours");
        Integer workedMinutes = summary.workedMinutes();
        if (workedMinutes != null) {
            if (!isWeekend && !isAbsence || workedMinutes > 0) {
                workedHours.setText(formatMinutes(workedMinutes));
            }
        }

        // Balance
        Span balance = new Span();
        balance.addClassName("balance");
        Integer balanceMinutes = summary.balanceMinutes();
        if (balanceMinutes != null) {
            if (!isWeekend && !isAbsence || balanceMinutes > 0) {
                balance.setText(formatBalance(balanceMinutes));
                balance.addClassName(balanceMinutes >= 0 ? "balance-positive" : "balance-negative");
            }
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
            if (!summary.date().isAfter(LocalDate.now())) {
                if (summary.targetMinutes() != null) {
                    totalTargetMinutes += summary.targetMinutes();
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
            if (!summary.date().isAfter(LocalDate.now())) {
                if (summary.targetMinutes() != null) {
                    totalTargetMinutes += summary.targetMinutes();
                }

                if (summary.workedMinutes() != null) {
                    totalWorkedMinutes += summary.workedMinutes();
                }
            }
        }

        monthStatisticsCard.updateYearStatistics(totalTargetMinutes, totalWorkedMinutes);
    }

    private String formatMinutes(Integer minutes) {
        int sign = minutes < 0 ? -1 : 1;
        int absMinutes = Math.abs(minutes);

        int hours = absMinutes / 60;
        int mins = absMinutes % 60;

        return String.format("%s%d:%02d h", sign < 0 ? "-" : "", hours, mins);
    }

    private String formatBalance(Integer minutes) {
        String sign = minutes >= 0 ? "+" : "";
        return sign + formatMinutes(minutes);
    }

    private String getDayTypeLabel(AbsenceType type) {
        return Optional.ofNullable(type).map(AbsenceType::name).orElse(null);
    }

    private void loadSummaries() {
        List<DaySummaryDTO> yearSummary = summaryService.getSummary(currentMonth.getYear());
        List<DaySummaryDTO> monthSummary = yearSummary.stream().filter(daySummaryDTO -> daySummaryDTO.date().getMonthValue() == currentMonth.getMonthValue()).toList();
        monthSummaries = monthSummary.stream().collect(Collectors.toMap(DaySummaryDTO::date, dto -> dto));
        yearSummaries = yearSummary.stream().collect(Collectors.toMap(DaySummaryDTO::date, dto -> dto));
    }
}