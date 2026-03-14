package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.service.TimeEntryValidationException;
import com.luxferre.chroniqo.service.TimeTrackingService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;


@Slf4j
@UIScope
@Component
public class TimeEntryDialog extends Dialog {

    private final DatePicker startDay = new DatePicker(LocalDate.now());
    private final DatePicker endDay = new DatePicker(LocalDate.now());
    private final TimePicker start = new TimePicker("Start Time");
    private final TimePicker end = new TimePicker("End Time");
    private final IntegerField breakMinutes = new IntegerField("Break");

    private final Tabs tabs = new Tabs();
    private final Tab workingTimeTab = new Tab("Working Time");
    private final Tab sickTab = new Tab("Sick");
    private final Tab vacationTab = new Tab("Vacation");
    private final VerticalLayout content = new VerticalLayout();

    // Error message display
    private final Div errorMessage = new Div();

    private final Button saveButton = new Button("Save");
    private final Button deleteButton = new Button("Delete");

    private final TimeTrackingService timeTrackingService;

    private final Binder<TimePicker> timePickerBinder = new Binder<>();


    public TimeEntryDialog(TimeTrackingService timeTrackingService) {
        this.timeTrackingService = timeTrackingService;
        initDialog();
        renderDialog();
        renderWorkingTime();
        timePickerBinder.forField(start)
                .withValidator(Objects::nonNull, "Start Time is mandatory!")
                .bind(TimePicker::getValue, TimePicker::setValue);
    }

    private void initDialog() {
        startDay.setLocale(UI.getCurrent().getLocale());
        endDay.setLocale(UI.getCurrent().getLocale());
        start.setLocale(UI.getCurrent().getLocale());
        end.setLocale(UI.getCurrent().getLocale());

        // Initialize error message component
        errorMessage.getStyle()
                .set("display", "none")
                .set("padding", "0.75rem 1rem")
                .set("margin-bottom", "1rem")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-error-color-10pct)")
                .set("border", "1px solid var(--lumo-error-color-50pct)")
                .set("color", "var(--lumo-error-text-color)");
    }

    void renderDialog() {
        // Dialog Styling
        addClassName("chroniqo-dialog");
        setWidth("500px");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        // Tabs
        tabs.add(workingTimeTab, vacationTab, sickTab);
        tabs.addSelectedChangeListener(event -> {
            clearErrorMessage(); // Clear errors when switching tabs
            if (workingTimeTab == event.getSelectedTab()) {
                renderWorkingTime();
            } else if (sickTab == event.getSelectedTab()) {
                renderSick();
            } else if (vacationTab == event.getSelectedTab()) {
                renderVacation();
            }
        });

        // Tabs Styling - Warm Dark
        tabs.getStyle()
                .set("background", "transparent")
                .set("border-bottom", "1px solid hsla(32, 30%, 50%, 0.15)");

        // Tab Styling
        workingTimeTab.getStyle()
                .set("color", "var(--lumo-body-text-color)");
        sickTab.getStyle()
                .set("color", "var(--lumo-body-text-color)");
        vacationTab.getStyle()
                .set("color", "var(--lumo-body-text-color)");

        // Break Minutes Field
        breakMinutes.setValue(30);
        breakMinutes.setStepButtonsVisible(true);
        breakMinutes.setStep(5);
        breakMinutes.setMin(0);
        breakMinutes.setMax(480);
        breakMinutes.setHelperText("Minutes");

        // Content Layout
        content.setAlignItems(FlexComponent.Alignment.STRETCH);
        content.setPadding(true);
        content.setSpacing(true);
        content.getStyle()
                .set("gap", "1rem");

        // Add error message at the top of content
        content.add(errorMessage, startDay, endDay, start, end, breakMinutes);

        // Buttons
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(this::save);

        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(this::delete);

        Button cancelButton = new Button("Cancel", e -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        // Header & Footer
        getHeader().add(tabs);
        add(content);
        getFooter().add(saveButton, deleteButton, cancelButton);

        // Footer Styling
        getFooter().getElement().getStyle()
                .set("gap", "0.5rem")
                .set("padding", "1rem")
                .set("background", "hsla(220, 20%, 10%, 0.5)")
                .set("border-top", "1px solid hsla(32, 30%, 50%, 0.15)");
    }

    public void renderWorkingTime() {
        startDay.setVisible(true);
        startDay.setLabel("Day");
        endDay.setVisible(false);
        start.setVisible(true);
        end.setVisible(true);
        breakMinutes.setVisible(true);

        // Update button styling
        saveButton.setText("Save");
        deleteButton.setText("Delete");
    }

    public void renderSick() {
        startDay.setVisible(true);
        startDay.setLabel("Start");
        endDay.setVisible(true);
        endDay.setLabel("End");
        start.setVisible(false);
        end.setVisible(false);
        breakMinutes.setVisible(false);

        // Update button styling
        saveButton.setText("Mark as Sick");
        deleteButton.setText("Remove Sick");
    }

    public void renderVacation() {
        startDay.setVisible(true);
        startDay.setLabel("Start");
        endDay.setVisible(true);
        endDay.setLabel("End");
        start.setVisible(false);
        end.setVisible(false);
        breakMinutes.setVisible(false);

        // Update button styling
        saveButton.setText("Book Vacation");
        deleteButton.setText("Remove Vacation");
    }

    public void open(LocalDate date) {
        reset();
        clearErrorMessage();
        startDay.setValue(date);
        endDay.setValue(date);
        TimeEntryDTO entry = timeTrackingService.getTimeEntry(date);
        tabs.setSelectedTab(workingTimeTab);

        if (null != entry) {
            start.setValue(entry.getStartTime());
            end.setValue(entry.getEndTime());
            breakMinutes.setValue(Optional.of(entry).map(TimeEntryDTO::getBreakMinutes).orElse(null));
            deleteButton.setVisible(true);
        } else {
            Absence absence = timeTrackingService.getAbsence(startDay.getValue());
            if (null != absence) {
                if (absence.getType() == AbsenceType.SICK) {
                    tabs.setSelectedTab(sickTab);
                    deleteButton.setVisible(true);
                } else if (absence.getType() == AbsenceType.VACATION) {
                    tabs.setSelectedTab(vacationTab);
                    deleteButton.setVisible(true);
                }
            } else {
                breakMinutes.setValue(30);
                deleteButton.setVisible(false);
            }
        }
        open();
    }

    private void reset() {
        startDay.setValue(null);
        endDay.setValue(null);
        start.setValue(null);
        end.setValue(null);
        breakMinutes.setValue(null);
        deleteButton.setVisible(false);
    }

    private void save(ClickEvent<Button> event) {
        clearErrorMessage();

        try {
            if (workingTimeTab.isSelected()) {
                saveWorkingTime();
                showSuccessNotification("Time entry saved successfully");
                close();
            } else if (sickTab.isSelected()) {
                saveSick();
                showSuccessNotification("Sick leave recorded");
                close();
            } else if (vacationTab.isSelected()) {
                saveVacation();
                showSuccessNotification("Vacation booked");
                close();
            }
        } catch (TimeEntryValidationException e) {
            // Show user-friendly error message
            showErrorMessage(e);
            log.warn("Validation error when saving time entry: {}", e.getMessage());
        } catch (Exception e) {
            // Unexpected error
            showErrorMessage("An unexpected error occurred. Please try again.");
            log.error("Unexpected error when saving time entry", e);
        }
    }

    private void delete(ClickEvent<Button> event) {
        clearErrorMessage();

        try {
            if (workingTimeTab.isSelected()) {
                deleteWorkingTime();
                showSuccessNotification("Time entry deleted");
                close();
            } else if (sickTab.isSelected()) {
                deleteSick();
                showSuccessNotification("Sick leave removed");
                close();
            } else if (vacationTab.isSelected()) {
                deleteVacation();
                showSuccessNotification("Vacation cancelled");
                close();
            }
        } catch (Exception e) {
            showErrorMessage("Failed to delete entry. Please try again.");
            log.error("Error when deleting time entry", e);
        }
    }

    private void saveWorkingTime() {
        timePickerBinder.validate();
        timeTrackingService.saveEntry(inputsToTimeEntryDto());
    }

    private void saveVacation() {
        timeTrackingService.saveAbsence(new AbsenceRequest(startDay.getValue(), endDay.getValue(), AbsenceType.VACATION));
    }

    private void saveSick() {
        timeTrackingService.saveAbsence(new AbsenceRequest(startDay.getValue(), endDay.getValue(), AbsenceType.SICK));
    }

    private void deleteWorkingTime() {
        timeTrackingService.deleteEntry(inputsToTimeEntryDto());
    }

    private void deleteVacation() {
        timeTrackingService.deleteAbsences(startDay.getValue(), endDay.getValue());
    }

    private void deleteSick() {
        timeTrackingService.deleteAbsences(startDay.getValue(), endDay.getValue());
    }

    private TimeEntryDTO inputsToTimeEntryDto() {
        TimeEntryDTO timeEntryDto = new TimeEntryDTO();
        timeEntryDto.setDate(startDay.getValue());
        timeEntryDto.setStartTime(start.getValue());
        timeEntryDto.setEndTime(end.getValue());
        timeEntryDto.setBreakMinutes(Optional.of(breakMinutes).map(IntegerField::getValue).orElse(null));
        return timeEntryDto;
    }

    /**
     * Shows an inline error message in the dialog
     */
    private void showErrorMessage(TimeEntryValidationException e) {
        String userFriendlyMessage = getUserFriendlyMessage(e);
        showErrorMessage(userFriendlyMessage);
    }

    /**
     * Shows an inline error message in the dialog
     */
    private void showErrorMessage(String message) {
        Icon icon = VaadinIcon.WARNING.create();
        icon.getStyle().set("margin-right", "0.5rem");

        HorizontalLayout messageLayout = new HorizontalLayout(icon, new Div(message));
        messageLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        messageLayout.setSpacing(false);
        messageLayout.getStyle().set("gap", "0");

        errorMessage.removeAll();
        errorMessage.add(messageLayout);
        errorMessage.getStyle().set("display", "block");
    }

    /**
     * Clears the inline error message
     */
    private void clearErrorMessage() {
        errorMessage.getStyle().set("display", "none");
        errorMessage.removeAll();
    }

    /**
     * Shows a success notification
     */
    private void showSuccessNotification(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    /**
     * Converts technical validation exception to user-friendly message
     */
    private String getUserFriendlyMessage(TimeEntryValidationException e) {
        return switch (e.getErrorType()) {
            case NULL_VALUE -> "Please fill in all required fields.";
            case FUTURE_DATE -> "You cannot log time for future dates.";
            case NEGATIVE_VALUE -> "Break time cannot be negative.";
            case VALUE_TOO_LARGE -> {
                if ("Break minutes".equals(e.getField())) {
                    yield "Break time cannot exceed 8 hours (480 minutes).";
                }
                yield "The value you entered is too large.";
            }
            case INVALID_RANGE -> {
                String msg = e.getMessage();
                if (msg.contains("must be after")) {
                    yield "End time must be after start time.";
                } else if (msg.contains("cannot be equal")) {
                    yield "End time cannot be the same as start time.";
                }
                yield "The time range you entered is invalid.";
            }
            case INCONSISTENT_DATA -> {
                String msg = e.getMessage();
                if (msg.contains("Break duration") && msg.contains("exceeds")) {
                    yield "Break time cannot be longer than total working time.";
                }
                yield "The data you entered is inconsistent. Please check your entries.";
            }
        };
    }
}