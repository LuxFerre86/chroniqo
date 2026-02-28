package com.luxferre.chroniqo.frontend.dashboard;

import com.luxferre.chroniqo.dto.WeeklyProgressDTO;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class QuickStatsWidget extends HorizontalLayout {

    private final VerticalLayout balanceBox;
    private final VerticalLayout weekProgressBox;
    private final Span balanceValue;
    private final Span progressValue;
    private final Div progressBar;

    public QuickStatsWidget() {
        addClassName("quick-stats-widget");
        setWidthFull();
        setSpacing(true);
        getStyle().set("gap", "1rem");

        // Balance Box
        balanceBox = createStatBox("Current Balance", "±0:00");
        balanceValue = (Span) balanceBox.getComponentAt(1);

        // Week Progress Box
        weekProgressBox = new VerticalLayout();
        weekProgressBox.setPadding(true);
        weekProgressBox.setSpacing(false);
        weekProgressBox.getStyle()
                .set("background", "linear-gradient(145deg, hsl(215, 22%, 12%) 0%, hsl(215, 22%, 10%) 100%)")
                .set("border", "1px solid hsla(38, 40%, 50%, 0.12)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 12px rgba(0, 0, 0, 0.5)")
                .set("padding", "1.5rem")
                .set("flex", "1");

        Span progressLabel = new Span("Weekly Progress");
        progressLabel.getStyle()
                .set("font-size", "12px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px")
                .set("font-weight", "600")
                .set("margin-bottom", "0.75rem");

        progressValue = new Span("0%");
        progressValue.getStyle()
                .set("font-size", "32px")
                .set("font-weight", "700")
                .set("color", "hsl(38, 95%, 65%)")
                .set("margin-bottom", "1rem");

        // Progress Bar
        Div progressBarContainer = new Div();
        progressBarContainer.getStyle()
                .set("width", "100%")
                .set("height", "8px")
                .set("background", "hsla(38, 20%, 50%, 0.15)")
                .set("border-radius", "4px")
                .set("overflow", "hidden")
                .set("position", "relative");

        progressBar = new Div();
        progressBar.getStyle()
                .set("width", "0%")
                .set("height", "100%")
                .set("background", "linear-gradient(90deg, hsl(38, 95%, 58%) 0%, hsl(35, 92%, 54%) 100%)")
                .set("border-radius", "4px")
                .set("box-shadow", "0 0 8px hsla(38, 92%, 50%, 0.5)")
                .set("transition", "width 0.5s ease");

        progressBarContainer.add(progressBar);

        Span progressDetails = new Span("0h / 40h");
        progressDetails.addClassName("progress-details");
        progressDetails.getStyle()
                .set("font-size", "12px")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("margin-top", "0.5rem");

        weekProgressBox.add(progressLabel, progressValue, progressBarContainer, progressDetails);

        add(balanceBox, weekProgressBox);
    }

    private VerticalLayout createStatBox(String label, String value) {
        VerticalLayout box = new VerticalLayout();
        box.setPadding(true);
        box.setSpacing(false);
        box.getStyle()
                .set("background", "linear-gradient(145deg, hsl(215, 22%, 12%) 0%, hsl(215, 22%, 10%) 100%)")
                .set("border", "1px solid hsla(38, 40%, 50%, 0.12)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 12px rgba(0, 0, 0, 0.5)")
                .set("padding", "1.5rem")
                .set("flex", "1");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "12px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px")
                .set("font-weight", "600")
                .set("margin-bottom", "0.75rem");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "32px")
                .set("font-weight", "700")
                .set("line-height", "1");

        box.add(labelSpan, valueSpan);
        return box;
    }

    public void updateBalance(int balanceMinutes) {
        balanceValue.setText(formatBalance(balanceMinutes));

        if (balanceMinutes >= 0) {
            balanceValue.getStyle()
                    .set("color", "hsl(142, 75%, 55%)");
        } else {
            balanceValue.getStyle()
                    .set("color", "hsl(12, 90%, 65%)");
        }
    }

    public void updateWeeklyProgress(WeeklyProgressDTO progress) {
        int percentage = progress.percentage();
        percentage = Math.min(100, Math.max(0, percentage));

        progressValue.setText(percentage + "%");
        progressBar.getStyle().set("width", percentage + "%");

        // Update progress details
        Span details = (Span) weekProgressBox.getComponentAt(3);
        details.setText(formatHours(progress.workedMinutes()) + " / " + formatHours(progress.targetMinutes()));

        // Color based on progress
        if (percentage >= 100) {
            progressValue.getStyle().set("color", "hsl(142, 75%, 55%)");
            progressBar.getStyle()
                    .set("background", "linear-gradient(90deg, hsl(142, 70%, 48%) 0%, hsl(145, 70%, 45%) 100%)");
        } else if (percentage >= 80) {
            progressValue.getStyle().set("color", "hsl(38, 95%, 65%)");
        } else {
            progressValue.getStyle().set("color", "hsl(12, 90%, 65%)");
        }
    }

    private String formatBalance(int minutes) {
        if (minutes == 0) return "0:00";
        String sign = minutes >= 0 ? "+" : "";
        int absMinutes = Math.abs(minutes);
        int hours = absMinutes / 60;
        int mins = absMinutes % 60;
        return String.format("%s%d:%02d", sign, hours, mins);
    }

    private String formatHours(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (mins == 0) {
            return hours + "h";
        }
        return String.format("%d:%02dh", hours, mins);
    }
}