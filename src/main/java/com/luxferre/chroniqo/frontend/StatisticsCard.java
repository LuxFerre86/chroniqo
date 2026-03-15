package com.luxferre.chroniqo.frontend;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Reusable UI card that displays target hours, worked hours, and balance for
 * both the current month and the current year side-by-side.
 *
 * <p>Values are updated independently via
 * {@link #updateMonthStatistics(int, int)} and
 * {@link #updateYearStatistics(int, int)}. Positive balances are rendered in
 * green, negative balances in red.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
public class StatisticsCard extends VerticalLayout {
    private final Span targetHoursValueMonth;
    private final Span targetHoursValueYear;
    private final Span workedHoursValueMonth;
    private final Span workedHoursValueYear;
    private final Span balanceValueMonth;
    private final Span balanceValueYear;

    public StatisticsCard() {
        addClassName("statistics-card");
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setWidthFull();
        statsLayout.setPadding(false);
        statsLayout.setSpacing(false);
        statsLayout.addClassName("stats-container");

        statsLayout.getStyle().set("display", "flex");
        statsLayout.getStyle().set("flex-wrap", "wrap");
        statsLayout.getStyle().set("gap", "1.5rem");
        statsLayout.setAlignItems(Alignment.START);

        // Target hours section
        VerticalLayout targetSection = createStatSection("Target Hours", "", "");
        targetHoursValueMonth = (Span) ((HorizontalLayout) targetSection.getComponentAt(1)).getComponentAt(0);
        targetHoursValueYear = (Span) ((HorizontalLayout) targetSection.getComponentAt(1)).getComponentAt(2);

        // Worked hours section
        VerticalLayout workedSection = createStatSection("Worked Hours", "", "");
        workedHoursValueMonth = (Span) ((HorizontalLayout) workedSection.getComponentAt(1)).getComponentAt(0);
        workedHoursValueYear = (Span) ((HorizontalLayout) workedSection.getComponentAt(1)).getComponentAt(2);

        // Balance section
        VerticalLayout balanceSection = createStatSection("Balance", "", "");
        balanceValueMonth = (Span) ((HorizontalLayout) balanceSection.getComponentAt(1)).getComponentAt(0);
        balanceValueYear = (Span) ((HorizontalLayout) balanceSection.getComponentAt(1)).getComponentAt(2);

        statsLayout.add(targetSection, workedSection, balanceSection);
        statsLayout.setFlexGrow(1, targetSection, workedSection, balanceSection);

        targetSection.getStyle().set("flex", "1 1 260px");
        workedSection.getStyle().set("flex", "1 1 260px");
        balanceSection.getStyle().set("flex", "1 1 260px");

        add(statsLayout);

        // Styling
        getStyle()
                .set("background", "linear-gradient(135deg, hsl(220, 20%, 14%) 0%, hsl(220, 20%, 12%) 100%)")
                .set("border", "1px solid hsla(32, 40%, 50%, 0.12)")
                .set("border-radius", "12px")
                .set("padding", "1.5rem")
                .set("box-shadow", "0 4px 12px rgba(0, 0, 0, 0.5), inset 0 1px 0 hsla(32, 50%, 70%, 0.05)");
    }

    private VerticalLayout createStatSection(String label, String monthValue, String yearValue) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setAlignItems(Alignment.CENTER);
        section.addClassName("stat-section");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("stat-label");
        labelSpan.getStyle()
                .set("font-size", "11px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "600")
                .set("margin-bottom", "0.75rem")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.8px");

        HorizontalLayout content = new HorizontalLayout();
        content.setSpacing(false);
        content.setPadding(false);
        content.setAlignItems(Alignment.BASELINE);
        content.addClassName("stat-content");
        content.getStyle()
                .set("gap", "8px");

        Span monthValueSpan = new Span(monthValue);
        monthValueSpan.addClassName("stat-month-value");
        monthValueSpan.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700")
                .set("line-height", "1")
                .set("color", "var(--lumo-body-text-color)");
        monthValueSpan.setTitle(label + " (monthly)");

        Span separator = new Span("/");
        separator.addClassName("stat-separator");
        separator.getStyle()
                .set("font-size", "20px")
                .set("font-weight", "400")
                .set("color", "var(--lumo-tertiary-text-color)")
                .set("opacity", "0.5");

        Span yearValueSpan = new Span(yearValue);
        yearValueSpan.addClassName("stat-year-value");
        yearValueSpan.getStyle()
                .set("font-size", "20px")
                .set("font-weight", "600")
                .set("line-height", "1")
                .set("color", "var(--lumo-secondary-text-color)");
        yearValueSpan.setTitle(label + " (yearly)");

        content.add(monthValueSpan, separator, yearValueSpan);

        section.add(labelSpan, content);
        return section;
    }

    /**
     * Updates the month-to-date column with fresh target and worked values.
     *
     * @param targetMinutes total target minutes for the month so far
     * @param workedMinutes total worked minutes for the month so far
     */
    public void updateMonthStatistics(int targetMinutes, int workedMinutes) {
        // Target hours
        targetHoursValueMonth.setText(formatMinutes(targetMinutes));
        targetHoursValueMonth.getStyle()
                .set("color", "var(--lumo-body-text-color)");

        // Worked hours
        workedHoursValueMonth.setText(formatMinutes(workedMinutes));
        workedHoursValueMonth.getStyle()
                .set("color", "hsl(32, 100%, 65%)")
                .set("text-shadow", "0 0 8px hsla(32, 95%, 58%, 0.3)");

        // Balance
        int balanceMinutes = workedMinutes - targetMinutes;
        balanceValueMonth.setText(formatBalance(balanceMinutes));

        if (balanceMinutes >= 0) {
            balanceValueMonth.getStyle()
                    .set("color", "hsl(145, 75%, 55%)")
                    .set("text-shadow", "0 0 8px hsla(145, 70%, 48%, 0.3)");
        } else {
            balanceValueMonth.getStyle()
                    .set("color", "hsl(8, 85%, 65%)")
                    .set("text-shadow", "0 0 8px hsla(8, 78%, 58%, 0.3)");
        }
    }

    /**
     * Updates the year-to-date column with fresh target and worked values.
     *
     * @param targetMinutes total target minutes for the year so far
     * @param workedMinutes total worked minutes for the year so far
     */
    public void updateYearStatistics(int targetMinutes, int workedMinutes) {
        // Target hours
        targetHoursValueYear.setText(formatMinutes(targetMinutes));
        targetHoursValueYear.getStyle()
                .set("color", "var(--lumo-secondary-text-color)");

        // Worked hours
        workedHoursValueYear.setText(formatMinutes(workedMinutes));
        workedHoursValueYear.getStyle()
                .set("color", "hsl(32, 90%, 60%)")
                .set("text-shadow", "0 0 6px hsla(32, 95%, 58%, 0.25)");

        // Balance
        int balanceMinutes = workedMinutes - targetMinutes;
        balanceValueYear.setText(formatBalance(balanceMinutes));

        if (balanceMinutes >= 0) {
            balanceValueYear.getStyle()
                    .set("color", "hsl(145, 70%, 50%)")
                    .set("text-shadow", "0 0 6px hsla(145, 70%, 48%, 0.25)");
        } else {
            balanceValueYear.getStyle()
                    .set("color", "hsl(8, 80%, 60%)")
                    .set("text-shadow", "0 0 6px hsla(8, 78%, 58%, 0.25)");
        }
    }

    private String formatMinutes(int minutes) {
        int sign = minutes < 0 ? -1 : 1;
        int absMinutes = Math.abs(minutes);

        int hours = absMinutes / 60;
        int mins = absMinutes % 60;

        return String.format("%s%d:%02d h", sign < 0 ? "-" : "", hours, mins);
    }

    private String formatBalance(int minutes) {
        String sign = minutes >= 0 ? "+" : "";
        return sign + formatMinutes(minutes);
    }
}