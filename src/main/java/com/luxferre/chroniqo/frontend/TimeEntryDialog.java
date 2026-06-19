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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
/**
 * Modal dialog for creating, editing, and deleting time entries and absences.
 *
 * <p>The dialog is organized as a state-driven workspace instead of a large
 * tabbed form. Working time is list-first with an inline editor for one booking
 * at a time, while sick leave and vacation use a compact range editor.
 * Validation errors from {@link TimeEntryValidationException} are displayed as
 * inline messages so the dialog can stay open for correction.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
@Slf4j
@UIScope
@Component
public class TimeEntryDialog extends Dialog {
    private static final int NOTES_PREVIEW_LENGTH = 40;
    private static final int DEFAULT_BREAK_MINUTES = 30;
    private static final String FOOTER_ACTION_BUTTON_WIDTH = "12.5rem";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private enum DialogState {
        WORKING_LIST,
        WORKING_EDIT,
        SICK_EDIT,
        VACATION_EDIT;
        boolean isWorking() {
            return this == WORKING_LIST || this == WORKING_EDIT;
        }
        boolean isAbsence() {
            return !isWorking();
        }
    }
    private final DatePicker startDay = new DatePicker();
    private final DatePicker endDay = new DatePicker();
    private final TimePicker start = new TimePicker("Start time");
    private final TimePicker end = new TimePicker("End time");
    private final IntegerField breakMinutes = new IntegerField("Break (min)");
    private final TextArea notes = new TextArea("Notes");
    private final Span headerDate = new Span();
    private final Span headerMode = new Span();
    private final Span headerSummary = new Span();
    private final Button workingModeButton = new Button("Working time");
    private final Button sickModeButton = new Button("Sick");
    private final Button vacationModeButton = new Button("Vacation");
    private final Div errorMessage = new Div();
    private final VerticalLayout content = new VerticalLayout();
    private final VerticalLayout workingSection = new VerticalLayout();
    private final VerticalLayout workingListPanel = new VerticalLayout();
    private final VerticalLayout workingList = new VerticalLayout();
    private final Div workingListEmptyState = new Div();
    private final Span workingSectionTitle = new Span("Bookings");
    private final Span workingListSummary = new Span();
    private final Span editorTitle = new Span();
    private final Span editorHint = new Span();
    private final VerticalLayout editorPanel = new VerticalLayout();
    private final Button addBookingButton = new Button("Add booking");
    private final VerticalLayout absenceSection = new VerticalLayout();
    private final Span absenceTitle = new Span();
    private final Span absenceHint = new Span();
    private final HorizontalLayout absenceDates = new HorizontalLayout();
    private final Button saveButton = new Button("Save booking");
    private final Button deleteButton = new Button("Delete booking");
    private final Button cancelButton = new Button("Close");
    private final TimeTrackingService timeTrackingService;
    private DialogState state = DialogState.WORKING_LIST;
    private AbsenceType absenceType;
    private LocalDate currentDate;
    private List<TimeEntryDTO> currentEntries = List.of();
    private String selectedEntryId;
    public TimeEntryDialog(TimeTrackingService timeTrackingService) {
        this.timeTrackingService = timeTrackingService;
        initDialog();
        buildDialog();
        switchToWorkingMode();
    }
    private void initDialog() {
        addClassName("chroniqo-dialog");

        startDay.setLocale(UI.getCurrent().getLocale());
        endDay.setLocale(UI.getCurrent().getLocale());
        start.setLocale(UI.getCurrent().getLocale());
        start.setStep(Duration.ofMinutes(5));
        end.setLocale(UI.getCurrent().getLocale());
        end.setStep(Duration.ofMinutes(5));
        startDay.setWidth("13rem");
        endDay.setWidth("13rem");
        start.setWidthFull();
        start.getStyle().set("flex-shrink", "0");
        end.setWidthFull();
        end.getStyle().set("flex-shrink", "0");
        breakMinutes.setWidthFull();
        breakMinutes.getStyle().set("flex-shrink", "0");
        breakMinutes.setStepButtonsVisible(true);
        breakMinutes.setStep(5);
        breakMinutes.setMin(0);
        breakMinutes.setMax(480);
        breakMinutes.setHelperText("Minutes");
        notes.setMaxLength(500);
        notes.setHelperText("Max. 500 characters");
        notes.setWidthFull();
        notes.setMinHeight("88px");
        notes.setMaxHeight("180px");
        notes.getStyle().set("flex-shrink", "0");
        start.setRequiredIndicatorVisible(true);
        errorMessage.getStyle()
                .set("display", "none")
                .set("padding", "0.75rem 1rem")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-error-color-10pct)")
                .set("border", "1px solid var(--lumo-error-color-50pct)")
                .set("color", "var(--lumo-error-text-color)");
        headerDate.getStyle()
                .set("font-size", "var(--lumo-font-size-xl)")
                .set("font-weight", "600");
        headerMode.getStyle()
                .set("padding", "0.2rem 0.55rem")
                .set("border-radius", "999px")
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "600");
        headerSummary.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        configureModeButton(workingModeButton, DialogState.WORKING_LIST);
        configureModeButton(sickModeButton, DialogState.SICK_EDIT);
        configureModeButton(vacationModeButton, DialogState.VACATION_EDIT);
        addBookingButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        addBookingButton.addClickListener(e -> switchToNewBookingMode());
        startDay.addValueChangeListener(event -> {
            if (!event.isFromClient() || event.getValue() == null) {
                return;
            }
            currentDate = event.getValue();
            if (state.isWorking()) {
                reloadWorkingEntries();
            } else if (state.isAbsence() && (endDay.getValue() == null || Objects.equals(endDay.getValue(), event.getOldValue()))) {
                endDay.setValue(event.getValue());
            }
            refreshHeader();
        });
        endDay.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                refreshHeader();
            }
        });
        start.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                refreshHeader();
            }
        });
        end.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                refreshHeader();
            }
        });
        breakMinutes.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                refreshHeader();
            }
        });
        notes.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                refreshHeader();
            }
        });
        content.setWidthFull();
        content.setPadding(true);
        content.setSpacing(true);
        content.setAlignItems(FlexComponent.Alignment.STRETCH);
        content.getStyle()
                .set("gap", "1rem")
                .set("min-height", "0")
                .set("overflow", "auto");
        workingSection.setWidthFull();
        workingSection.setPadding(false);
        workingSection.setSpacing(true);
        workingSection.setAlignItems(FlexComponent.Alignment.STRETCH);
        workingListPanel.setPadding(false);
        workingListPanel.setSpacing(true);
        workingListPanel.setAlignItems(FlexComponent.Alignment.STRETCH);
        workingListPanel.getStyle()
                .set("flex", "1 1 360px")
                .set("min-width", "0")
                .set("padding", "1rem")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background", "var(--lumo-base-color)")
                .set("display", "flex")
                .set("flex-direction", "column");
        workingList.setPadding(false);
        workingList.setSpacing(false);
        workingList.setWidthFull();
        workingList.getStyle()
                .set("gap", "0.5rem")
                .set("flex", "1 1 auto")
                .set("min-height", "0")
                .set("overflow", "auto");
        workingListSummary.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        workingSectionTitle.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600");
        workingListEmptyState.getStyle()
                .set("padding", "0.75rem 0")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-style", "italic");
        editorPanel.setPadding(false);
        editorPanel.setSpacing(true);
        editorPanel.setAlignItems(FlexComponent.Alignment.STRETCH);
        editorPanel.getStyle()
                .set("flex", "1 1 320px")
                .set("min-width", "0")
                .set("padding", "1rem")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "1rem");
        editorTitle.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("display", "block")
                .set("min-height", "1.5rem")
                .set("line-height", "1.5rem");
        editorHint.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("display", "block")
                .set("min-height", "2.4rem")
                .set("line-height", "1.2")
                .set("white-space", "normal");
        // ...existing code...
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(this::save);
        saveButton.getStyle().set("min-width", FOOTER_ACTION_BUTTON_WIDTH);
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(this::delete);
        deleteButton.getStyle().set("min-width", FOOTER_ACTION_BUTTON_WIDTH);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> close());
        cancelButton.getStyle().set("min-width", FOOTER_ACTION_BUTTON_WIDTH);
        absenceSection.setWidthFull();
        absenceSection.setPadding(false);
        absenceSection.setSpacing(true);
        absenceSection.setAlignItems(FlexComponent.Alignment.STRETCH);
        absenceSection.getStyle()
                .set("padding", "1rem")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("background", "var(--lumo-base-color)");
        absenceTitle.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600");
        absenceHint.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");
        absenceDates.setWidthFull();
        absenceDates.setPadding(false);
        absenceDates.setSpacing(true);
        absenceDates.setAlignItems(FlexComponent.Alignment.END);
        absenceDates.getStyle().set("gap", "0.75rem");
        // Action buttons stay in the footer to keep the dialog position stable.
        HorizontalLayout headerRow = new HorizontalLayout(headerDate, headerMode);
        headerRow.setWidthFull();
        headerRow.setPadding(false);
        headerRow.setSpacing(true);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        VerticalLayout headerColumn = new VerticalLayout(headerRow, headerSummary, buildModeSwitchRow());
        headerColumn.setWidthFull();
        headerColumn.setPadding(false);
        headerColumn.setSpacing(false);
        headerColumn.getStyle().set("gap", "0.5rem");
        getHeader().add(headerColumn);
        add(content);
        getFooter().add(saveButton, deleteButton, cancelButton);
        getFooter().getElement().getStyle()
                .set("gap", "0.5rem")
                .set("padding", "1rem")
                .set("background", "hsla(220, 20%, 10%, 0.5)")
                .set("border-top", "1px solid hsla(32, 30%, 50%, 0.15)");
        setWidth("min(920px, 96vw)");
        setHeight("min(780px, 92vh)");
        setMinHeight("min(620px, 88vh)");
        setMaxWidth("96vw");
        setMaxHeight("90vh");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
    }

    private void buildDialog() {
        workingListPanel.addClassName("time-entry-working-list-panel");
        editorPanel.addClassName("time-entry-editor-panel");
        absenceDates.addClassName("time-entry-absence-dates");

        VerticalLayout workingWorkspace = new VerticalLayout(workingSectionTitle, workingListSummary, workingList, addBookingButton);
        workingWorkspace.setPadding(false);
        workingWorkspace.setSpacing(true);
        workingWorkspace.setWidthFull();
        workingWorkspace.getStyle()
                .set("flex", "1 1 auto")
                .set("display", "flex")
                .set("flex-direction", "column");
        workingWorkspace.setAlignItems(FlexComponent.Alignment.STRETCH);

        workingListPanel.add(workingWorkspace);

        editorPanel.add(editorTitle, editorHint, start, end, breakMinutes, notes);
        HorizontalLayout workingWorkspaceRow = new HorizontalLayout(workingListPanel, editorPanel);
        workingWorkspaceRow.addClassName("time-entry-working-row");
        workingWorkspaceRow.setWidthFull();
        workingWorkspaceRow.setPadding(false);
        workingWorkspaceRow.setSpacing(true);
        workingWorkspaceRow.getStyle()
                .set("flex-wrap", "wrap")
                .set("align-items", "flex-start");
        workingSection.add(workingWorkspaceRow);

        absenceDates.add(startDay, endDay);
        absenceSection.add(absenceTitle, absenceHint, absenceDates);

        content.add(errorMessage, workingSection, absenceSection);
    }

    private HorizontalLayout buildModeSwitchRow() {
        HorizontalLayout modeSwitch = new HorizontalLayout(workingModeButton, sickModeButton, vacationModeButton);
        modeSwitch.addClassName("time-entry-mode-switch");
        modeSwitch.setWidthFull();
        modeSwitch.setPadding(false);
        modeSwitch.setSpacing(true);
        modeSwitch.getStyle().set("gap", "0.5rem");
        modeSwitch.expand(workingModeButton, sickModeButton, vacationModeButton);
        return modeSwitch;
    }
    private void configureModeButton(Button button, DialogState targetState) {
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        button.addClickListener(event -> {
            clearErrorMessage();
            if (targetState == DialogState.WORKING_LIST) {
                switchToWorkingMode();
            } else {
                switchToAbsenceMode(targetState == DialogState.SICK_EDIT ? AbsenceType.SICK : AbsenceType.VACATION);
            }
        });
    }
    /**
     * Opens the dialog pre-populated with the existing entry or absence for the
     * given date.
     *
     * @param date the calendar date to view or edit
     */
    public void open(LocalDate date) {
        Objects.requireNonNull(date, "date");
        reset();
        currentDate = date;
        startDay.setValue(date);
        endDay.setValue(date);
        List<TimeEntryDTO> entries = timeTrackingService.getTimeEntries(date);
        Absence absence = entries.isEmpty() ? timeTrackingService.getAbsence(date) : null;
        if (absence != null) {
            absenceType = absence.getType();
            switchToAbsenceMode(absenceType);
            refreshHeader();
        } else {
            absenceType = null;
            currentEntries = entries;
            switchToWorkingMode();
            renderWorkingEntries();
            refreshHeader();
        }
        super.open();
    }
    private void reset() {
        currentDate = null;
        currentEntries = List.of();
        selectedEntryId = null;
        absenceType = null;
        state = DialogState.WORKING_LIST;
        startDay.clear();
        endDay.clear();
        start.clear();
        end.clear();
        breakMinutes.clear();
        notes.clear();
        workingList.removeAll();
        workingListEmptyState.removeAll();
        clearErrorMessage();
    }
    private void switchToWorkingMode() {
        state = selectedEntryId == null ? DialogState.WORKING_LIST : DialogState.WORKING_EDIT;
        absenceType = null;
        startDay.setVisible(true);
        startDay.setLabel("Date");
        endDay.setVisible(false);
        workingSection.setVisible(true);
        absenceSection.setVisible(false);
        renderWorkingEntries();
        updateWorkingEditor();
        updateActions();
        refreshHeader();
    }
    private void switchToAbsenceMode(AbsenceType type) {
        state = type == AbsenceType.SICK ? DialogState.SICK_EDIT : DialogState.VACATION_EDIT;
        absenceType = type;
        selectedEntryId = null;
        startDay.setVisible(true);
        startDay.setLabel("Start date");
        endDay.setVisible(true);
        endDay.setLabel("End date");
        workingSection.setVisible(false);
        absenceSection.setVisible(true);
        updateAbsencePanel();
        updateActions();
        refreshHeader();
    }
    private void switchToNewBookingMode() {
        selectedEntryId = null;
        state = DialogState.WORKING_LIST;
        clearWorkingEditor();
        renderWorkingEntries();
        updateWorkingEditor();
        updateActions();
        refreshHeader();
    }
    private void save(ClickEvent<Button> event) {
        clearErrorMessage();
        try {
            if (state.isWorking()) {
                saveWorkingTime();
                showSuccessNotification("Time entry saved successfully");
                reloadWorkingEntries();
            } else if (state == DialogState.SICK_EDIT) {
                saveAbsence(AbsenceType.SICK);
                showSuccessNotification("Sick leave recorded");
                close();
            } else if (state == DialogState.VACATION_EDIT) {
                saveAbsence(AbsenceType.VACATION);
                showSuccessNotification("Vacation booked");
                close();
            }
        } catch (TimeEntryValidationException e) {
            showErrorMessage(e);
            log.warn("Validation error when saving time entry: {}", e.getMessage());
        } catch (Exception e) {
            showErrorMessage("An unexpected error occurred. Please try again.");
            log.error("Unexpected error when saving time entry", e);
        }
    }
    private void delete(ClickEvent<Button> event) {
        clearErrorMessage();
        try {
            if (state.isWorking()) {
                if (selectedEntryId == null) {
                    showErrorMessage("Select a booking to delete first.");
                    return;
                }
                deleteWorkingTime(selectedEntryId);
                showSuccessNotification("Time entry deleted");
                reloadWorkingEntries();
                switchToNewBookingMode();
            } else if (state == DialogState.SICK_EDIT) {
                deleteAbsence();
                showSuccessNotification("Sick leave removed");
                close();
            } else if (state == DialogState.VACATION_EDIT) {
                deleteAbsence();
                showSuccessNotification("Vacation cancelled");
                close();
            }
        } catch (Exception e) {
            showErrorMessage("Failed to delete entry. Please try again.");
            log.error("Error when deleting time entry", e);
        }
    }
    private void saveWorkingTime() {
        if (startDay.getValue() == null) {
            throw TimeEntryValidationException.nullValue("date", "Date cannot be null");
        }
        if (start.getValue() == null) {
            throw TimeEntryValidationException.nullValue("startTime", "Start time is mandatory");
        }
        currentDate = startDay.getValue();
        timeTrackingService.saveEntry(inputsToTimeEntryDto());
    }
    private void saveAbsence(AbsenceType type) {
        validateAbsenceRange();
        timeTrackingService.saveAbsence(new AbsenceRequest(startDay.getValue(), endDay.getValue(), type));
    }
    private void deleteWorkingTime(String entryId) {
        timeTrackingService.deleteEntry(inputsToTimeEntryDto(entryId));
    }
    private void deleteAbsence() {
        validateAbsenceRange();
        timeTrackingService.deleteAbsences(startDay.getValue(), endDay.getValue());
    }
    private TimeEntryDTO inputsToTimeEntryDto() {
        return inputsToTimeEntryDto(selectedEntryId);
    }
    private TimeEntryDTO inputsToTimeEntryDto(String entryId) {
        TimeEntryDTO timeEntryDto = new TimeEntryDTO();
        timeEntryDto.setId(entryId);
        timeEntryDto.setDate(startDay.getValue());
        timeEntryDto.setStartTime(start.getValue());
        timeEntryDto.setEndTime(end.getValue());
        timeEntryDto.setBreakMinutes(breakMinutes.getValue());
        String notesValue = notes.getValue();
        timeEntryDto.setNotes(notesValue != null && !notesValue.isBlank() ? notesValue.trim() : null);
        return timeEntryDto;
    }
    private void validateAbsenceRange() {
        if (startDay.getValue() == null) {
            throw TimeEntryValidationException.nullValue("startDate", "Start date cannot be null");
        }
        if (endDay.getValue() == null) {
            throw TimeEntryValidationException.nullValue("endDate", "End date cannot be null");
        }
        if (startDay.getValue().isAfter(endDay.getValue())) {
            throw TimeEntryValidationException.invalidRange(
                    "End date must be on or after start date",
                    startDay.getValue(),
                    endDay.getValue());
        }
    }
    private void reloadWorkingEntries() {
        if (currentDate == null) {
            currentEntries = List.of();
            renderWorkingEntries();
            refreshHeader();
            return;
        }
        currentEntries = timeTrackingService.getTimeEntries(currentDate);
        renderWorkingEntries();
        updateWorkingEditor();
        updateActions();
        refreshHeader();
    }
    private void renderWorkingEntries() {
        workingList.removeAll();
        workingListEmptyState.removeAll();
        if (currentEntries.isEmpty()) {
            workingListEmptyState.setText("No bookings yet for this day. Use Add booking to create the first one.");
            workingList.add(workingListEmptyState);
            workingListSummary.setText("No bookings yet");
            return;
        }
        workingListSummary.setText(buildWorkingSummary(currentEntries));
        currentEntries.stream()
                .sorted(Comparator.comparing(TimeEntryDTO::getStartTime, Comparator.nullsLast(LocalTime::compareTo)))
                .forEach(this::addEntryRow);
    }
    private void addEntryRow(TimeEntryDTO entry) {
        Div row = new Div();
        row.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("padding", "0.75rem")
                .set("cursor", "pointer")
                .set("background", Objects.equals(selectedEntryId, entry.getId())
                        ? "var(--lumo-primary-color-10pct)"
                        : "transparent");
        HorizontalLayout rowLayout = new HorizontalLayout();
        rowLayout.setWidthFull();
        rowLayout.setPadding(false);
        rowLayout.setSpacing(true);
        rowLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        rowLayout.getStyle()
                .set("gap", "0.75rem");
        VerticalLayout entryText = new VerticalLayout();
        entryText.setPadding(false);
        entryText.setSpacing(false);
        entryText.getStyle()
                .set("gap", "0.2rem")
                .set("flex", "1 1 auto")
                .set("min-width", "0");
        Span headline = new Span(formatEntryHeadline(entry));
        headline.getStyle()
                .set("font-weight", "600")
                .set("font-family", "monospace")
                .set("display", "block")
                .set("min-height", "1.25rem");
        Span details = new Span(formatEntryDetails(entry));
        details.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("display", "block")
                .set("min-height", "1.25rem");
        entryText.add(headline, details);
        HorizontalLayout actions = new HorizontalLayout();
        actions.setPadding(false);
        actions.setSpacing(false);
        actions.getStyle()
                .set("gap", "0.25rem")
                .set("flex", "0 0 auto")
                .set("align-items", "center");
        Button editButton = buildIconButton(VaadinIcon.EDIT, "Edit booking", e -> applyEntryToEditor(entry));
        Button deleteIconButton = buildIconButton(VaadinIcon.TRASH, "Delete booking", e -> deleteWorkingTime(entry.getId()));
        actions.add(editButton, deleteIconButton);
        rowLayout.add(entryText, actions);
        row.add(rowLayout);
        row.getElement().addEventListener("click", event -> applyEntryToEditor(entry));
        workingList.add(row);
    }
    private Button buildIconButton(VaadinIcon iconType, String title, java.util.function.Consumer<ClickEvent<Button>> action) {
        Button button = new Button(iconType.create());
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        button.getElement().setAttribute("title", title);
        button.getElement().setAttribute("aria-label", title);
        button.getStyle()
                .set("min-width", "2.25rem")
                .set("padding", "0.25rem");
        button.addClickListener(action::accept);
        stopClickPropagation(button);
        return button;
    }
    private void stopClickPropagation(Button button) {
        button.getElement().executeJs("this.addEventListener('click', function(e) { e.stopPropagation(); });");
    }
    private String formatEntryHeadline(TimeEntryDTO entry) {
        String startText = entry.getStartTime() != null ? entry.getStartTime().toString() : "--:--";
        String endText = entry.getEndTime() != null ? entry.getEndTime().toString() : "OPEN";
        return startText + " - " + endText;
    }
    private String formatEntryDetails(TimeEntryDTO entry) {
        StringBuilder details = new StringBuilder();
        Integer pause = entry.getBreakMinutes();
        if (pause != null) {
            details.append("Break ").append(pause).append("m");
        }
        String notesText = entry.getNotes();
        if (notesText != null && !notesText.isBlank()) {
            if (!details.isEmpty()) {
                details.append(" · ");
            }
            details.append(abbreviate(notesText.trim()));
        }
        return details.isEmpty() ? "No extra details" : details.toString();
    }
    private String buildWorkingSummary(List<TimeEntryDTO> entries) {
        int workedMinutes = entries.stream().mapToInt(this::calculateWorkedMinutes).sum();
        long openBookings = entries.stream().filter(entry -> entry.getStartTime() != null && entry.getEndTime() == null).count();
        StringBuilder summary = new StringBuilder();
        summary.append(entries.size()).append(entries.size() == 1 ? " booking" : " bookings");
        if (workedMinutes > 0) {
            summary.append(" · ").append(formatMinutes(workedMinutes)).append(" worked");
        }
        if (openBookings > 0) {
            summary.append(" · ").append(openBookings).append(openBookings == 1 ? " open booking" : " open bookings");
        }
        return summary.toString();
    }
    private int calculateWorkedMinutes(TimeEntryDTO entry) {
        if (entry.getStartTime() == null || entry.getEndTime() == null) {
            return 0;
        }
        long totalMinutes = Duration.between(entry.getStartTime(), entry.getEndTime()).toMinutes();
        int breakValue = entry.getBreakMinutes() != null ? entry.getBreakMinutes() : 0;
        return Math.max(0, Math.toIntExact(totalMinutes - breakValue));
    }
    private String formatMinutes(int minutes) {
        int sign = minutes < 0 ? -1 : 1;
        int absMinutes = Math.abs(minutes);
        int hours = absMinutes / 60;
        int mins = absMinutes % 60;
        return String.format("%s%d:%02d h", sign < 0 ? "-" : "", hours, mins);
    }
    private void applyEntryToEditor(TimeEntryDTO entry) {
        selectedEntryId = entry.getId();
        state = DialogState.WORKING_EDIT;
        currentDate = entry.getDate();
        startDay.setValue(entry.getDate());
        start.setValue(entry.getStartTime());
        end.setValue(entry.getEndTime());
        breakMinutes.setValue(entry.getBreakMinutes());
        notes.setValue(entry.getNotes() != null ? entry.getNotes() : "");
        updateWorkingEditor();
        updateActions();
        renderWorkingEntries();
        refreshHeader();
    }
    private void clearWorkingEditor() {
        selectedEntryId = null;
        start.setValue(null);
        end.setValue(null);
        breakMinutes.setValue(DEFAULT_BREAK_MINUTES);
        notes.setValue("");
    }
    private void updateWorkingEditor() {
        if (selectedEntryId == null) {
            state = DialogState.WORKING_LIST;
            editorTitle.setText("New booking");
            editorHint.setText("Create a new booking for the selected day.");
            saveButton.setText("Save changes");
            deleteButton.setVisible(false);
            clearWorkingEditor();
        } else {
            editorTitle.setText("Editing booking");
            editorHint.setText("Update the selected booking or delete it if it is no longer needed.");
            saveButton.setText("Save changes");
            deleteButton.setText("Delete booking");
            deleteButton.setVisible(true);
        }
    }
    private void updateAbsencePanel() {
        absenceTitle.setText(absenceType == AbsenceType.SICK ? "Sick leave" : "Vacation");
        absenceHint.setText("Use a simple date range. No time fields are needed for absences.");
        saveButton.setText("Save changes");
        deleteButton.setText(absenceType == AbsenceType.SICK ? "Remove sick leave" : "Remove vacation");
        deleteButton.setVisible(true);
        if (startDay.getValue() != null) {
            startDay.setValue(startDay.getValue());
        }
        if (endDay.getValue() == null && startDay.getValue() != null) {
            endDay.setValue(startDay.getValue());
        }
        absenceDates.removeAll();
        absenceDates.add(startDay, endDay);
        absenceSection.removeAll();
        absenceSection.add(absenceTitle, absenceHint, absenceDates);
    }
    private void updateActions() {
        if (state.isWorking()) {
            saveButton.setVisible(true);
            deleteButton.setVisible(selectedEntryId != null);
            editorPanel.removeAll();
            editorPanel.add(editorTitle, editorHint, start, end, breakMinutes, notes);
        } else {
            saveButton.setVisible(true);
            deleteButton.setVisible(true);
            absenceSection.removeAll();
            absenceSection.add(absenceTitle, absenceHint, absenceDates);
        }
    }
    private void refreshHeader() {
        LocalDate date = startDay.getValue() != null ? startDay.getValue() : currentDate;
        headerDate.setText(date != null ? formatDate(date) : "");
        if (state == DialogState.SICK_EDIT) {
            headerMode.setText("Sick leave");
            headerSummary.setText(buildAbsenceSummary());
        } else if (state == DialogState.VACATION_EDIT) {
            headerMode.setText("Vacation");
            headerSummary.setText(buildAbsenceSummary());
        } else {
            headerMode.setText("Working time");
            headerSummary.setText(buildWorkingHeaderSummary());
        }
        updateModeButtonState(workingModeButton, state.isWorking());
        updateModeButtonState(sickModeButton, state == DialogState.SICK_EDIT);
        updateModeButtonState(vacationModeButton, state == DialogState.VACATION_EDIT);
    }

    private void updateModeButtonState(Button button, boolean active) {
        if (active) {
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            button.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
        } else {
            button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        }
    }

    private String buildWorkingHeaderSummary() {
        if (currentEntries.isEmpty()) {
            return "No bookings yet for this day";
        }
        return buildWorkingSummary(currentEntries);
    }
    private String buildAbsenceSummary() {
        LocalDate startDate = startDay.getValue();
        LocalDate endDate = endDay.getValue();
        if (startDate == null) {
            return "";
        }
        if (endDate == null || Objects.equals(startDate, endDate)) {
            return formatDate(startDate);
        }
        return formatDate(startDate) + " – " + formatDate(endDate);
    }
    private String formatDate(LocalDate date) {
        return DATE_FORMATTER.format(date);
    }
    private void showErrorMessage(TimeEntryValidationException e) {
        String userFriendlyMessage = getUserFriendlyMessage(e);
        showErrorMessage(userFriendlyMessage);
    }
    private void showErrorMessage(String message) {
        Icon icon = VaadinIcon.WARNING.create();
        icon.getStyle().set("margin-right", "0.5rem");
        HorizontalLayout messageLayout = new HorizontalLayout(icon, new Div(new Span(message)));
        messageLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        messageLayout.setSpacing(false);
        messageLayout.getStyle().set("gap", "0");
        errorMessage.removeAll();
        errorMessage.add(messageLayout);
        errorMessage.getStyle().set("display", "block");
    }
    private void clearErrorMessage() {
        errorMessage.getStyle().set("display", "none");
        errorMessage.removeAll();
    }
    private void showSuccessNotification(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
    private String getUserFriendlyMessage(TimeEntryValidationException e) {
        return switch (e.getErrorType()) {
            case NULL_VALUE -> {
                if ("startTime".equals(e.getField())) {
                    yield "Please enter a start time.";
                }
                if ("date".equals(e.getField()) || "startDate".equals(e.getField())) {
                    yield "Please select a start date.";
                }
                if ("endDate".equals(e.getField())) {
                    yield "Please select an end date.";
                }
                yield "Please fill in all required fields.";
            }
            case FUTURE_DATE -> "You cannot log time for future dates.";
            case NEGATIVE_VALUE -> "Break time cannot be negative.";
            case VALUE_TOO_LARGE -> {
                if ("Break minutes".equals(e.getField())) {
                    yield "Break time cannot exceed 8 hours (480 minutes).";
                }
                if ("Notes".equals(e.getField())) {
                    yield "Notes cannot exceed 500 characters.";
                }
                yield "The value you entered is too large.";
            }
            case INVALID_RANGE -> {
                String msg = e.getMessage();
                if (msg.contains("overlaps")) {
                    yield "This time range overlaps with an existing booking.";
                } else if (msg.contains("date")) {
                    yield "End date must be on or after start date.";
                } else if (msg.contains("must be after")) {
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
    private String abbreviate(String value) {
        if (value == null || value.length() <= NOTES_PREVIEW_LENGTH) {
            return value;
        }
        return value.substring(0, NOTES_PREVIEW_LENGTH - 1) + "...";
    }
}

