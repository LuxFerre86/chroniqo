package com.luxferre.chroniqo.frontend;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

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

        // Container für die Statistiken
        HorizontalLayout statsLayout = new HorizontalLayout();
        statsLayout.setWidthFull();
        statsLayout.setPadding(false);
        statsLayout.setSpacing(false);
        statsLayout.addClassName("aura-stats-container");

        // Soll-Stunden
        VerticalLayout targetSection = createStatSection("Target Hours", "", "");
        targetHoursValueMonth = (Span) ((HorizontalLayout) targetSection.getComponentAt(1)).getComponentAt(0);
        targetHoursValueYear = (Span) ((HorizontalLayout) targetSection.getComponentAt(1)).getComponentAt(2);

        // Ist-Stunden
        VerticalLayout workedSection = createStatSection("Worked Hours", "", "");
        workedHoursValueMonth = (Span) ((HorizontalLayout) workedSection.getComponentAt(1)).getComponentAt(0);
        workedHoursValueYear = (Span) ((HorizontalLayout) workedSection.getComponentAt(1)).getComponentAt(2);

        // Bilanz
        VerticalLayout balanceSection = createStatSection("Balance", "", "");
        balanceValueMonth = (Span) ((HorizontalLayout) balanceSection.getComponentAt(1)).getComponentAt(0);
        balanceValueYear = (Span) ((HorizontalLayout) balanceSection.getComponentAt(1)).getComponentAt(2);

        statsLayout.add(targetSection, workedSection, balanceSection);
        statsLayout.setFlexGrow(1, targetSection, workedSection, balanceSection);

        add(statsLayout);

        // Aura Styling
        getStyle()
                .set("background", "var(--aura-surface-0)")
                .set("border", "1px solid var(--aura-surface-border)")
                .set("border-radius", "var(--aura-border-radius)")
                .set("padding", "1.5rem");
    }

    private VerticalLayout createStatSection(String label, String monthValue, String yearValue) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        section.setAlignItems(Alignment.CENTER);
        section.addClassName("aura-stat-section");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("aura-stat-label");
        labelSpan.getStyle()
                .set("font-size", "var(--aura-font-size-sm)")
                .set("color", "var(--aura-text-color-secondary)")
                .set("font-weight", "500")
                .set("margin-bottom", "0.5rem")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.025em");

        HorizontalLayout content = new HorizontalLayout();
        Span monthValueSpan = new Span(monthValue);
        monthValueSpan.addClassName("aura-stat-monthValue");
        monthValueSpan.getStyle()
                .set("font-size", "var(--aura-font-size-2xl)")
                .set("font-weight", "700")
                .set("line-height", "1.2");
        monthValueSpan.setTitle(label + " (monthly)");

        Span separator = new Span("/");

        Span yearValueSpan = new Span(yearValue);
        yearValueSpan.addClassName("aura-stat-monthValue");
        yearValueSpan.getStyle()
                .set("font-size", "var(--aura-font-size-2xl)")
                .set("font-weight", "700")
                .set("line-height", "1.2");
        yearValueSpan.setTitle(label + " (yearly)");
        content.add(monthValueSpan, separator, yearValueSpan);

        section.add(labelSpan, content);
        return section;
    }

    public void updateMonthStatistics(int targetMinutes, int workedMinutes) {
        // Soll-Stunden
        targetHoursValueMonth.setText(formatMinutes(targetMinutes));
        targetHoursValueMonth.getStyle().set("color", "var(--aura-text-color)");

        // Ist-Stunden
        workedHoursValueMonth.setText(formatMinutes(workedMinutes));
        workedHoursValueMonth.getStyle().set("color", "var(--aura-primary-500)");

        // Bilanz
        int balanceMinutes = workedMinutes - targetMinutes;
        balanceValueMonth.setText(formatBalance(balanceMinutes));

        if (balanceMinutes >= 0) {
            balanceValueMonth.getStyle().set("color", "var(--aura-green-text)");
        } else {
            balanceValueMonth.getStyle().set("color", "var(--aura-red-text)");
        }
    }

    public void updateYearStatistics(int targetMinutes, int workedMinutes) {
        // Soll-Stunden
        targetHoursValueYear.setText(formatMinutes(targetMinutes));
        targetHoursValueYear.getStyle().set("color", "var(--aura-text-color)");

        // Ist-Stunden
        workedHoursValueYear.setText(formatMinutes(workedMinutes));
        workedHoursValueYear.getStyle().set("color", "var(--aura-primary-500)");

        // Bilanz
        int balanceMinutes = workedMinutes - targetMinutes;
        balanceValueYear.setText(formatBalance(balanceMinutes));

        if (balanceMinutes >= 0) {
            balanceValueYear.getStyle().set("color", "var(--aura-green-text)");
        } else {
            balanceValueYear.getStyle().set("color", "var(--aura-red-text)");
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