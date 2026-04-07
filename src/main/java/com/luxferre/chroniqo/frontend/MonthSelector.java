package com.luxferre.chroniqo.frontend;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import lombok.Getter;
import lombok.Setter;

import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Reusable navigation component for selecting a year-month.
 *
 * <p>Renders previous/next chevron buttons flanking a clickable month label.
 * Clicking the label opens a modal picker with a year drop-down and a
 * 3-column month grid. A "Today" shortcut button appears to the right of the
 * navigation row on the same line whenever the displayed month is not the
 * current calendar month. Changes are notified to the owner via
 * {@link MonthChangeListener}.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
public class MonthSelector extends VerticalLayout {

    @Getter
    private YearMonth selectedMonth;
    private final Span monthLabel;
    private final Button todayButton;
    @Setter
    private MonthChangeListener changeListener;

    public MonthSelector() {
        this.selectedMonth = YearMonth.now();
        addClassName("month-selector");

        // ── Navigation row ────────────────────────────────────────────────
        Button previousButton = new Button(VaadinIcon.CHEVRON_LEFT.create());
        previousButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        previousButton.addClickListener(e -> navigateToPreviousMonth());
        previousButton.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("border-radius", "8px");

        // Month label – clicking opens the month-picker dialog
        monthLabel = new Span(formatMonth(selectedMonth));
        monthLabel.addClassName("month-label");
        monthLabel.getStyle()
                .set("cursor", "pointer")
                .set("padding", "0.625rem 1.5rem")
                .set("border-radius", "8px")
                .set("font-weight", "600")
                .set("font-size", "18px")
                .set("min-width", "220px")
                .set("text-align", "center")
                .set("display", "inline-block")
                .set("color", "var(--lumo-body-text-color)")
                .set("background", "linear-gradient(135deg, hsl(220, 20%, 14%) 0%, hsl(220, 20%, 12%) 100%)")
                .set("border", "1px solid hsla(32, 40%, 50%, 0.15)")
                .set("box-shadow", "0 2px 6px rgba(0, 0, 0, 0.3)")
                .set("transition", "all 0.2s ease");

        monthLabel.addClickListener(e -> openMonthPickerDialog());

        // Hover effect
        monthLabel.getElement().addEventListener("mouseenter", e -> monthLabel.getStyle()
                .set("background", "linear-gradient(135deg, hsl(220, 20%, 16%) 0%, hsl(220, 20%, 14%) 100%)")
                .set("border-color", "hsla(32, 40%, 50%, 0.25)")
                .set("box-shadow", "0 4px 12px rgba(0, 0, 0, 0.4)")
                .set("transform", "translateY(-1px)"));

        monthLabel.getElement().addEventListener("mouseleave", e -> monthLabel.getStyle()
                .set("background", "linear-gradient(135deg, hsl(220, 20%, 14%) 0%, hsl(220, 20%, 12%) 100%)")
                .set("border-color", "hsla(32, 40%, 50%, 0.15)")
                .set("box-shadow", "0 2px 6px rgba(0, 0, 0, 0.3)")
                .set("transform", "translateY(0)"));

        // Navigate to next month
        Button nextButton = new Button(VaadinIcon.CHEVRON_RIGHT.create());
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        nextButton.addClickListener(e -> navigateToNextMonth());
        nextButton.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("border-radius", "8px");

        HorizontalLayout navRow = new HorizontalLayout(previousButton, monthLabel, nextButton);
        navRow.setAlignItems(FlexComponent.Alignment.CENTER);
        navRow.setSpacing(true);
        navRow.setPadding(false);
        navRow.getStyle()
                .set("gap", "0.75rem")
                .set("grid-column", "2")
                .set("grid-row", "1")
                .set("justify-self", "center");

        // ── Today button (right on desktop, above nav on mobile) ──────────
        // Styled as a warm-amber pill to match the app's dark warm theme
        todayButton = new Button("Today");
        todayButton.addClassName("today-btn");
        todayButton.getStyle()
                .set("background", "hsla(32, 60%, 50%, 0.08)")
                .set("border", "1px solid hsla(32, 50%, 50%, 0.25)")
                .set("color", "hsl(32, 85%, 65%)")
                .set("border-radius", "8px")
                .set("font-size", "12px")
                .set("font-weight", "500")
                .set("padding", "0.2rem 1rem")
                .set("cursor", "pointer")
                .set("transition", "background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease")
                .set("box-shadow", "0 1px 3px rgba(0, 0, 0, 0.2)")
                .set("grid-column", "3")
                .set("grid-row", "1")
                .set("justify-self", "start")   // left-aligned in right column → closest to nav
                .set("align-self", "center");   // same vertical height as month label

        todayButton.getElement().addEventListener("mouseenter", e -> todayButton.getStyle()
                .set("background", "hsla(32, 60%, 50%, 0.15)")
                .set("border-color", "hsla(32, 60%, 55%, 0.45)")
                .set("box-shadow", "0 2px 6px rgba(0, 0, 0, 0.3)"));
        todayButton.getElement().addEventListener("mouseleave", e -> todayButton.getStyle()
                .set("background", "hsla(32, 60%, 50%, 0.08)")
                .set("border-color", "hsla(32, 50%, 50%, 0.25)")
                .set("box-shadow", "0 1px 3px rgba(0, 0, 0, 0.2)"));

        todayButton.addClickListener(e -> navigateToCurrentMonth());
        // Use CSS visibility (not setVisible) so the reserved space never collapses
        todayButton.getStyle().set("visibility",
                selectedMonth.equals(YearMonth.now()) ? "hidden" : "visible");

        // ── Outer layout: 3-column grid on desktop, stacked on mobile ─────
        setWidthFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr auto 1fr")
                .set("align-items", "center")
                .set("column-gap", "2rem");

        add(navRow, todayButton);
    }

    private void navigateToPreviousMonth() {
        selectedMonth = selectedMonth.minusMonths(1);
        updateLabel();
        updateTodayButtonVisibility();
        fireChangeEvent();
    }

    private void navigateToNextMonth() {
        selectedMonth = selectedMonth.plusMonths(1);
        updateLabel();
        updateTodayButtonVisibility();
        fireChangeEvent();
    }

    /**
     * Navigates to the current calendar month and fires the change listener.
     */
    void navigateToCurrentMonth() {
        selectedMonth = YearMonth.now();
        updateLabel();
        updateTodayButtonVisibility();
        fireChangeEvent();
    }

    private void updateTodayButtonVisibility() {
        todayButton.getStyle().set("visibility", selectedMonth.equals(YearMonth.now()) ? "hidden" : "visible");
    }

    private void openMonthPickerDialog() {
        Dialog dialog = new Dialog();
        dialog.addClassName("month-picker-dialog");
        dialog.setWidth("400px");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.getStyle().set("gap", "1rem");

        // Dialog title
        Span title = new Span("Select Month");
        title.getStyle()
                .set("font-size", "16px")
                .set("font-weight", "600")
                .set("color", "var(--lumo-body-text-color)")
                .set("margin-bottom", "0.5rem");

        // Year selector
        Select<Integer> yearSelect = new Select<>();
        yearSelect.setLabel("Year");
        yearSelect.setItems(IntStream.rangeClosed(2000, 2050).boxed().toList());
        yearSelect.setValue(selectedMonth.getYear());
        yearSelect.setWidthFull();

        // Re-render month grid whenever the year changes
        Div monthGrid = createMonthGrid(dialog, yearSelect);

        yearSelect.addValueChangeListener(e -> {
            content.remove(monthGrid);
            Div newMonthGrid = createMonthGrid(dialog, yearSelect);
            content.add(newMonthGrid);
        });

        content.add(title, yearSelect, monthGrid);
        content.setHorizontalComponentAlignment(Alignment.CENTER, title);

        dialog.add(content);
        dialog.open();
    }

    private Div createMonthGrid(Dialog dialog, Select<Integer> yearSelect) {
        Div monthGrid = new Div();
        monthGrid.addClassName("month-grid");
        monthGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(3, 1fr)")
                .set("gap", "0.5rem")
                .set("width", "100%");

        for (Month month : Month.values()) {
            Button monthButton = new Button(
                    month.getDisplayName(TextStyle.SHORT, Locale.UK)
            );
            monthButton.setWidthFull();

            // Highlight the currently selected month
            YearMonth buttonMonth = YearMonth.of(yearSelect.getValue(), month);
            if (buttonMonth.equals(selectedMonth)) {
                monthButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                monthButton.getStyle()
                        .set("background", "linear-gradient(135deg, hsl(32, 95%, 58%) 0%, hsl(28, 95%, 54%) 100%)")
                        .set("color", "hsl(220, 25%, 10%)")
                        .set("font-weight", "600")
                        .set("box-shadow", "0 3px 8px hsla(32, 95%, 50%, 0.3)");
            } else if (buttonMonth.equals(YearMonth.now())) {
                // Current month (not selected)
                monthButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                monthButton.getStyle()
                        .set("border", "1px solid hsla(32, 60%, 50%, 0.3)")
                        .set("color", "var(--lumo-primary-text-color)");
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
        return monthName + " " + yearMonth.getYear();
    }

    private void fireChangeEvent() {
        if (changeListener != null) {
            changeListener.onMonthChanged(selectedMonth);
        }
    }

    /**
     * Programmatically sets the selected month and updates the displayed label
     * and Today-button visibility. Does not fire the {@link MonthChangeListener}.
     *
     * @param yearMonth the month to select
     */
    public void setSelectedMonth(YearMonth yearMonth) {
        this.selectedMonth = yearMonth;
        updateLabel();
        updateTodayButtonVisibility();
    }

    @FunctionalInterface
    public interface MonthChangeListener {
        void onMonthChanged(YearMonth newMonth);
    }
}





