package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.dto.WeeklyProgressDTO;
import com.luxferre.chroniqo.frontend.dashboard.QuickStatsWidget;
import com.luxferre.chroniqo.frontend.dashboard.TodaySummaryCard;
import com.luxferre.chroniqo.frontend.dashboard.WeekChartWidget;
import com.luxferre.chroniqo.service.SummaryService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Main dashboard view shown at the application root ({@code "/"}).
 *
 * <p>Aggregates today's summary, the current-week bar chart, the running
 * balance, and the weekly progress indicator. All widgets are refreshed on
 * load and whenever the {@link TimeEntryDialog} is closed after a save or
 * delete operation.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
@Route(value = "", layout = AppLayoutBasic.class)
@PageTitle("Dashboard | chroniqo")
@PermitAll
public class DashboardView extends VerticalLayout {

    private final SummaryService summaryService;
    private final TimeEntryDialog timeEntryDialog;
    private Set<Registration> registrations;

    // Widgets
    private final TodaySummaryCard todaySummaryCard;
    private final WeekChartWidget weekChartWidget;
    private final QuickStatsWidget quickStatsWidget;

    public DashboardView(SummaryService summaryService, TimeEntryDialog timeEntryDialog) {
        this.summaryService = summaryService;
        this.timeEntryDialog = timeEntryDialog;

        addClassName("dashboard-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Page Header
        HorizontalLayout header = createHeader();

        // Main Grid Layout
        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setPadding(false);
        mainContent.setSpacing(true);
        mainContent.setMaxWidth("1400px");
        mainContent.getStyle()
                .set("gap", "1.5rem")
                .set("width", "100%");

        // Row 1: Today's Summary (Large Card)
        todaySummaryCard = new TodaySummaryCard();
        todaySummaryCard.setWidthFull();

        // Row 2: Quick Actions
        HorizontalLayout quickActions = createQuickActions();

        // Row 3: Stats Row (Balance + Week Progress)
        quickStatsWidget = new QuickStatsWidget();

        // Row 4: Week Chart
        weekChartWidget = new WeekChartWidget();
        weekChartWidget.setWidthFull();

        mainContent.add(
                todaySummaryCard,
                quickActions,
                quickStatsWidget,
                weekChartWidget
        );

        add(header, mainContent);
        setHorizontalComponentAlignment(Alignment.CENTER, mainContent);

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        registrations = summaryService.register(event -> ui.access(this::refreshDashboard));
        refreshDashboard();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        Optional.ofNullable(registrations).ifPresent(regs -> regs.forEach(Registration::remove));
        registrations = null;
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
                .set("margin-bottom", "1rem");

        // Title & Date
        VerticalLayout titleSection = new VerticalLayout();
        titleSection.setPadding(false);
        titleSection.setSpacing(false);

        H2 title = new H2("Dashboard");
        title.getStyle()
                .set("margin", "0")
                .set("color", "hsl(38, 95%, 65%)")
                .set("font-weight", "700");

        String todayFormatted = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.UK));
        Span dateLabel = new Span(todayFormatted);
        dateLabel.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "14px");

        titleSection.add(title, dateLabel);

        // Refresh Button
        Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> refreshDashboard());

        header.add(titleSection, refreshButton);
        return header;
    }

    private HorizontalLayout createQuickActions() {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setSpacing(true);
        actions.getStyle().set("gap", "1rem");

        // Log Full Day Button
        Button logFullButton = new Button("Log Time", VaadinIcon.CLOCK.create());
        logFullButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        logFullButton.getStyle()
                .set("flex", "1");
        logFullButton.addClickListener(e -> timeEntryDialog.open(LocalDate.now()));

        // View Month Button
        Button viewMonthButton = new Button("View Month", VaadinIcon.CALENDAR.create());
        viewMonthButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        viewMonthButton.getStyle()
                .set("flex", "1");
        viewMonthButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(MonthView.class)));

        actions.add(logFullButton, viewMonthButton);
        return actions;
    }

    private void refreshDashboard() {
        // Today's Summary
        DaySummaryDTO todaySummary = summaryService.getToday();
        todaySummaryCard.updateSummary(todaySummary);

        // Week Chart
        List<DaySummaryDTO> weekData = summaryService.getCurrentWeek();
        weekChartWidget.updateChart(weekData);

        // Balance
        quickStatsWidget.updateBalance(getCurrentBalance());

        // Weekly Progress
        WeeklyProgressDTO weeklyProgressDTO = summaryService.getWeeklyProgress();
        quickStatsWidget.updateWeeklyProgress(weeklyProgressDTO);
    }

    /**
     * Get current balance
     */
    int getCurrentBalance() {
        LocalDate today = LocalDate.now();
        return summaryService.getSummary(today.getYear())
                .stream()
                .filter(s -> s.date().isBefore(today) || s.date().equals(today))
                .mapToInt(DaySummaryDTO::balanceMinutes)
                .sum();
    }
}