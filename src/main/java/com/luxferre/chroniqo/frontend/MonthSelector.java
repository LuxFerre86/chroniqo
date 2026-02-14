package com.luxferre.chroniqo.frontend;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;

import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.IntStream;

public class MonthSelector extends HorizontalLayout {

    private YearMonth selectedMonth;
    private final Span monthLabel;
    private MonthChangeListener changeListener;

    public MonthSelector() {
        this.selectedMonth = YearMonth.now();

        // Navigation Button zurück
        Button previousButton = new Button(VaadinIcon.CHEVRON_LEFT.create());
        previousButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        previousButton.addClickListener(e -> navigateToPreviousMonth());

        // Monatslabel (klickbar)
        monthLabel = new Span(formatMonth(selectedMonth));
        monthLabel.getStyle()
                .set("cursor", "pointer")
                .set("padding", "0.5rem 1rem")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("font-weight", "500")
                .set("min-width", "200px")
                .set("text-align", "center")
                .set("display", "inline-block");

        monthLabel.addClickListener(e -> openMonthPickerDialog());

        // Hover-Effekt
        monthLabel.getElement().addEventListener("mouseenter", e -> {
            monthLabel.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        });
        monthLabel.getElement().addEventListener("mouseleave", e -> {
            monthLabel.getStyle().set("background-color", "transparent");
        });

        // Navigation Button vorwärts
        Button nextButton = new Button(VaadinIcon.CHEVRON_RIGHT.create());
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addClickListener(e -> navigateToNextMonth());

        // Layout konfigurieren
        setAlignItems(Alignment.CENTER);
        setSpacing(false);
        add(previousButton, monthLabel, nextButton);
    }

    private void navigateToPreviousMonth() {
        selectedMonth = selectedMonth.minusMonths(1);
        updateLabel();
        fireChangeEvent();
    }

    private void navigateToNextMonth() {
        selectedMonth = selectedMonth.plusMonths(1);
        updateLabel();
        fireChangeEvent();
    }

    private void openMonthPickerDialog() {
        Dialog dialog = new Dialog();

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        // Jahr-Auswahl
        Select<Integer> yearSelect = new Select<>();
        yearSelect.setItems(IntStream.rangeClosed(2000, 2050).boxed().toList());
        yearSelect.setValue(selectedMonth.getYear());
        //yearSelect.setWidthFull();

        // Monats-Grid
        Div monthGrid = createMonthGrid(dialog, yearSelect);

        content.add(yearSelect, monthGrid);
        content.setHorizontalComponentAlignment(Alignment.CENTER, yearSelect);
        dialog.add(content);

        dialog.open();
    }

    private Div createMonthGrid(Dialog dialog, Select<Integer> yearSelect) {
        Div monthGrid = new Div();
        monthGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(3, 1fr)")
                .set("gap", "0.5rem")
                .set("margin-top", "1rem");

        for (Month month : Month.values()) {
            Button monthButton = new Button(
                    month.getDisplayName(TextStyle.FULL, Locale.UK)
            );
            monthButton.setWidthFull();

            // Aktuellen Monat hervorheben
            YearMonth buttonMonth = YearMonth.of(yearSelect.getValue(), month);
            if (buttonMonth.equals(selectedMonth)) {
                monthButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            } else {
                monthButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            }

            monthButton.addClickListener(e -> {
                selectedMonth = YearMonth.of(yearSelect.getValue(), month);
                updateLabel();
                fireChangeEvent();
                dialog.close();
            });

            monthGrid.add(monthButton);
        }

        return monthGrid;
    }

    private void updateLabel() {
        monthLabel.setText(formatMonth(selectedMonth));
    }

    private String formatMonth(YearMonth yearMonth) {
        String monthName = yearMonth.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.UK);
        // Ersten Buchstaben großschreiben
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
        return monthName + " " + yearMonth.getYear();
    }

    private void fireChangeEvent() {
        if (changeListener != null) {
            changeListener.onMonthChanged(selectedMonth);
        }
    }

    // Getter und Setter
    public YearMonth getSelectedMonth() {
        return selectedMonth;
    }

    public void setSelectedMonth(YearMonth yearMonth) {
        this.selectedMonth = yearMonth;
        updateLabel();
    }

    public void setChangeListener(MonthChangeListener listener) {
        this.changeListener = listener;
    }

    @FunctionalInterface
    public interface MonthChangeListener {
        void onMonthChanged(YearMonth newMonth);
    }
}