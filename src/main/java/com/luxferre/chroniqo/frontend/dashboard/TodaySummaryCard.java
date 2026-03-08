package com.luxferre.chroniqo.frontend.dashboard;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class TodaySummaryCard extends VerticalLayout {

    private final Span workedHoursValue;
    private final Span balanceValue;

    public TodaySummaryCard() {
        addClassName("today-summary-card");
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        // Styling
        getStyle()
                .set("background", "linear-gradient(145deg, hsl(215, 22%, 12%) 0%, hsl(215, 22%, 10%) 100%)")
                .set("border", "1px solid hsla(38, 40%, 50%, 0.15)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 6px 20px rgba(0, 0, 0, 0.6)")
                .set("padding", "2rem")
                .set("min-height", "200px");

        // Date Label
        Span dateLabel = new Span("Today");
        dateLabel.getStyle()
                .set("font-size", "14px")
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "1px")
                .set("margin-bottom", "1rem");

        // Worked Hours (Large Display)
        workedHoursValue = new Span("0:00");
        workedHoursValue.getStyle()
                .set("font-size", "64px")
                .set("font-weight", "700")
                .set("color", "hsl(38, 95%, 65%)")
                .set("line-height", "1")
                .set("text-shadow", "0 4px 12px hsla(38, 92%, 50%, 0.4)");

        Span hoursLabel = new Span("hours worked");
        hoursLabel.getStyle()
                .set("font-size", "14px")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("margin-top", "0.5rem")
                .set("margin-bottom", "1.5rem");

        // Balance
        balanceValue = new Span("+0:00");
        balanceValue.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "600")
                .set("padding", "0.5rem 1.5rem")
                .set("border-radius", "8px");

        add(dateLabel, workedHoursValue, hoursLabel, balanceValue);
    }

    public void updateSummary(DaySummaryDTO summary) {
        if (summary == null) {
            workedHoursValue.setText("0:00");
            balanceValue.setText("0:00");
            return;
        }

        // Update worked hours
        workedHoursValue.setText(formatMinutes(summary.workedMinutes()));

        // Update balance
        balanceValue.setText(formatBalance(summary.balanceMinutes()));

        // Update balance styling
        if (summary.balanceMinutes() >= 0) {
            balanceValue.getStyle()
                    .set("color", "hsl(142, 75%, 55%)")
                    .set("background", "hsla(142, 70%, 48%, 0.15)")
                    .set("box-shadow", "inset 0 0 0 1px hsla(142, 70%, 48%, 0.25)");
        } else {
            balanceValue.getStyle()
                    .set("color", "hsl(12, 90%, 65%)")
                    .set("background", "hsla(12, 85%, 58%, 0.15)")
                    .set("box-shadow", "inset 0 0 0 1px hsla(12, 85%, 58%, 0.25)");
        }
    }

    private String formatMinutes(Integer minutes) {
        if (minutes == null) return "0:00";

        int absMinutes = Math.abs(minutes);
        int hours = absMinutes / 60;
        int mins = absMinutes % 60;

        return String.format("%d:%02d", hours, mins);
    }

    private String formatBalance(Integer minutes) {
        if (minutes == null) return "0:00";
        String sign = minutes >= 0 ? "+" : "";
        return sign + formatMinutes(minutes);
    }
}