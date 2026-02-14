package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.service.MonthService;
import com.luxferre.chroniqo.service.YearService;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Route("month")
@UIScope
@Component
public class MonthView extends VerticalLayout {
    private YearMonth currentMonth = YearMonth.now();
    private final Div calendarGrid;
    private Span monthLabel;
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

        // Statistik-Card erstellen
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
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        };

        for (DayOfWeek day : weekdays) {
            Div header = new Div();
            header.setText(day.getDisplayName(TextStyle.SHORT, Locale.GERMAN));
            header.addClassName("calendar-header");

            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                header.addClassName("calendar-weekend");
            }

            calendarGrid.add(header);
        }

        // ===== Tage =====
        LocalDate firstDay = currentMonth.atDay(1);
        int firstWeekdayIndex = firstDay.getDayOfWeek().getValue() - 1; // Mo=0

        // Leere Zellen vor Monatsbeginn
        for (int i = 0; i < firstWeekdayIndex; i++) {
            calendarGrid.add(new Div());
        }

        // Monatstage
        for (int day = 1; day <= currentMonth.lengthOfMonth(); day++) {
            LocalDate date = currentMonth.atDay(day);

            calendarGrid.add(createDayCard(date));
        }
    }

    private Card createDayCard(LocalDate date) {
        DaySummaryDTO summary = monthSummaries.get(date);

        Card dayCard = new Card();

        // Manueller Header mit day-number
        Div dayHeader = new Div();
        dayHeader.addClassName("day-header");

        Span dayNumber = new Span(String.valueOf(date.getDayOfMonth()));
        dayNumber.addClassName("day-number");

        // WICHTIG: data-weekday für Mobile-Ansicht
        String weekdayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.GERMAN);
        dayNumber.getElement().setAttribute("data-weekday", weekdayName);

        dayHeader.add(dayNumber);

        if (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dayCard.addClassName("calendar-weekend");
        }

        // Day Content Container
        Div dayContent = new Div();
        dayContent.addClassName("day-content");

        dayContent.add(new Div(formatMinutes(summary.workedMinutes())));

        Div balance = new Div(formatBalance(summary.balanceMinutes()));
        if (summary.balanceMinutes() != null) {
            balance.addClassName(
                    summary.balanceMinutes() >= 0
                            ? "balance-positive"
                            : "balance-negative"
            );
        } else {
            balance.addClassName("balance-neutral");
        }
        dayContent.add(balance);

        String dayTypeLabel = getDayTypeLabel(summary.absenceType());
        if (StringUtils.hasText(dayTypeLabel)) {
            Span badge = new Span(dayTypeLabel);
            badge.addClassName("day-badge");
            badge.addClassName(getDayTypeClassName(summary.absenceType()));
            dayCard.setHeaderSuffix(badge);
        }

        // Alles zur Card hinzufügen
        dayCard.setHeaderPrefix(dayHeader);
        dayCard.add(dayContent);

        dayCard.getElement().addEventListener("click", event -> {
            timeEntryDialog.open(date);
        });
        dayCard.getStyle().set("cursor", "pointer");

        return dayCard;
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
                // Soll-Stunden: nur Werktage ohne Abwesenheit
                if (summary.absenceType() == null &&
                        summary.date().getDayOfWeek() != DayOfWeek.SATURDAY &&
                        summary.date().getDayOfWeek() != DayOfWeek.SUNDAY) {
                    // Annahme: 8 Stunden Soll-Arbeitszeit pro Tag (480 Minuten)
                    // Dies kannst du anpassen, falls die Soll-Zeit aus dem DTO kommt
                    totalTargetMinutes += 468;
                }

                // Ist-Stunden
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
            // Soll-Stunden: nur Werktage ohne Abwesenheit
            if (summary.date().isBefore(LocalDate.now())) {
                if (summary.absenceType() == null &&
                        summary.date().getDayOfWeek() != DayOfWeek.SATURDAY &&
                        summary.date().getDayOfWeek() != DayOfWeek.SUNDAY) {
                    // Annahme: 8 Stunden Soll-Arbeitszeit pro Tag (480 Minuten)
                    // Dies kannst du anpassen, falls die Soll-Zeit aus dem DTO kommt
                    totalTargetMinutes += 468;
                }

                // Ist-Stunden
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

    private String getDayTypeClassName(AbsenceType type) {
        if (null == type) {
            return null;
        } else {
            return switch (type) {
                case HOLIDAY -> "day-badge--holiday";
                case VACATION -> "day-badge--vacation";
                case SICK -> "day-badge--sick";
            };
        }
    }

    private void loadSummaries() {
        monthSummaries = monthService.getMonth(currentMonth.getYear(), currentMonth.getMonthValue()).stream().collect(Collectors.toMap(DaySummaryDTO::date, dto -> dto));
        yearSummaries = yearService.getYear(currentMonth.getYear()).stream().collect(Collectors.toMap(DaySummaryDTO::date, dto -> dto));
    }
}
