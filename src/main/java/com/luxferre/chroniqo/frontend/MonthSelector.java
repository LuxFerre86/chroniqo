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
 * 3-column month grid. Changes are notified to the owner via
 * {@link MonthChangeListener}.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
public class MonthSelector extends HorizontalLayout {

    @Getter
    private YearMonth selectedMonth;
    private final Span monthLabel;
    @Setter
    private MonthChangeListener changeListener;

    public MonthSelector() {
        this.selectedMonth = YearMonth.now();
        addClassName("month-selector");

        // Navigate to previous month
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

        // Configure layout
        setAlignItems(Alignment.CENTER);
        setSpacing(true);
        getStyle()
                .set("gap", "0.75rem");

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
     * Programmatically sets the selected month and updates the displayed label.
     * Does not fire the {@link MonthChangeListener}.
     *
     * @param yearMonth the month to select
     */
    public void setSelectedMonth(YearMonth yearMonth) {
        this.selectedMonth = yearMonth;
        updateLabel();
    }

    @FunctionalInterface
    public interface MonthChangeListener {
        void onMonthChanged(YearMonth newMonth);
    }
}