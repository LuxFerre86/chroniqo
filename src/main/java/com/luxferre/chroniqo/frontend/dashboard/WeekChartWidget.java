package com.luxferre.chroniqo.frontend.dashboard;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class WeekChartWidget extends VerticalLayout {

    private final HorizontalLayout chartBars;

    public WeekChartWidget() {
        addClassName("week-chart-widget");
        setPadding(true);
        setSpacing(true);

        // Styling
        getStyle()
                .set("background", "linear-gradient(145deg, hsl(215, 22%, 12%) 0%, hsl(215, 22%, 10%) 100%)")
                .set("border", "1px solid hsla(38, 40%, 50%, 0.12)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 12px rgba(0, 0, 0, 0.5)")
                .set("padding", "1.5rem");

        // Title
        Span title = new Span("Current Week");
        title.getStyle()
                .set("font-size", "18px")
                .set("font-weight", "600")
                .set("color", "var(--lumo-body-text-color)")
                .set("margin-bottom", "1rem");

        // Chart Container
        chartBars = new HorizontalLayout();
        chartBars.setWidthFull();
        chartBars.setHeight("180px");
        chartBars.setSpacing(true);
        chartBars.setAlignItems(Alignment.END);
        chartBars.getStyle()
                .set("gap", "8px");

        add(title, chartBars);
    }

    public void updateChart(List<DaySummaryDTO> weekData) {
        chartBars.removeAll();

        if (weekData == null || weekData.isEmpty()) {
            return;
        }

        // Find max hours for scaling
        int maxMinutes = weekData.stream()
                .filter(d -> d.workedMinutes() != null)
                .mapToInt(DaySummaryDTO::workedMinutes)
                .max()
                .orElse(480); // Default 8 hours

        maxMinutes = Math.max(maxMinutes, 480); // At least 8 hours scale

        for (DaySummaryDTO day : weekData) {
            chartBars.add(createBar(day, maxMinutes));
        }
    }

    private VerticalLayout createBar(DaySummaryDTO day, int maxMinutes) {
        VerticalLayout barContainer = new VerticalLayout();
        barContainer.setPadding(false);
        barContainer.setSpacing(false);
        barContainer.setAlignItems(Alignment.CENTER);
        barContainer.setHeightFull();
        barContainer.getStyle()
                .set("flex", "1")
                .set("min-width", "0");

        // === BAR AREA ===
        VerticalLayout barArea = new VerticalLayout();
        barArea.setPadding(false);
        barArea.setSpacing(false);
        barArea.setWidthFull();
        barArea.setHeightFull();
        barArea.setJustifyContentMode(JustifyContentMode.END);
        barArea.setAlignItems(Alignment.CENTER);

        Div bar = new Div();

        int workedMinutes = day.workedMinutes() != null ? day.workedMinutes() : 0;
        int heightPercent = maxMinutes > 0 ? (workedMinutes * 100) / maxMinutes : 0;
        heightPercent = Math.min(100, Math.max(0, heightPercent));

        bar.getStyle()
                .set("width", "100%")
                .set("height", heightPercent + "%")
                .set("min-height", workedMinutes > 0 ? "8px" : "2px")
                .set("background", workedMinutes > 0
                        ? "linear-gradient(180deg, hsl(38, 95%, 58%) 0%, hsl(35, 92%, 54%) 100%)"
                        : "hsla(38, 20%, 50%, 0.2)")
                .set("border-radius", "6px 6px 0 0");

        barArea.add(bar);

        // === LABEL AREA ===
        VerticalLayout labelArea = new VerticalLayout();
        labelArea.setPadding(false);
        labelArea.setSpacing(false);
        labelArea.setAlignItems(Alignment.CENTER);
        labelArea.getStyle().set("margin-top", "0.5rem");

        // Day Label
        String dayName = day.date().getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        Span dayLabel = new Span(dayName);
        dayLabel.getStyle()
                .set("font-size", "14px")
                .set("font-weight", "600")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("text-transform", "uppercase")
                .set("margin-top", "0.5rem");

        // Hours Label
        Span hoursLabel = new Span(formatHours(workedMinutes));
        hoursLabel.getStyle()
                .set("font-size", "13px")
                .set("color", "var(--lumo-disabled-text-color)")
                .set("margin-top", "2px");

        labelArea.add(dayLabel, hoursLabel);

        barContainer.add(barArea, labelArea);

        return barContainer;
    }

    private String formatHours(int minutes) {
        if (minutes == 0) return "0h";
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (mins == 0) {
            return hours + "h";
        }
        return String.format("%d:%02dh", hours, mins);
    }
}