package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.service.AbsenceService;
import com.luxferre.chroniqo.service.TimeEntryService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;


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

    private final Button saveButton = new Button("Save");
    private final Button deleteButton = new Button("Delete");

    private final TimeEntryService timeEntryService;
    private final AbsenceService absenceService;

    private final Binder<TimePicker> timePickerBinder = new Binder<>();


    public TimeEntryDialog(TimeEntryService timeEntryService, AbsenceService absenceService) {
        this.timeEntryService = timeEntryService;
        this.absenceService = absenceService;
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
        content.add(startDay, endDay, start, end, breakMinutes);

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
        startDay.setValue(date);
        endDay.setValue(date);
        TimeEntryDTO entry = timeEntryService.getEntry(date);
        tabs.setSelectedTab(workingTimeTab);

        if (null != entry) {
            start.setValue(Optional.of(entry).map(TimeEntryDTO::getStartTime).map(LocalTime::parse).orElse(null));
            end.setValue(Optional.of(entry).map(TimeEntryDTO::getEndTime).map(LocalTime::parse).orElse(null));
            breakMinutes.setValue(Optional.of(entry).map(TimeEntryDTO::getBreakMinutes).orElse(null));
            deleteButton.setVisible(true);
        } else {
            Absence absence = absenceService.getAbsence(startDay.getValue());
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
        if (workingTimeTab.isSelected()) {
            saveWorkingTime();
            close();
        } else if (sickTab.isSelected()) {
            saveSick();
            close();
        } else if (vacationTab.isSelected()) {
            saveVacation();
            close();
        }
    }

    private void delete(ClickEvent<Button> event) {
        if (workingTimeTab.isSelected()) {
            deleteWorkingTime();
            close();
        } else if (sickTab.isSelected()) {
            deleteSick();
            close();
        } else if (vacationTab.isSelected()) {
            deleteVacation();
            close();
        }
    }

    private void saveWorkingTime() {
        timePickerBinder.validate();
        timeEntryService.saveEntry(inputsToTimeEntryDto());
    }

    private void saveVacation() {
        absenceService.saveAbsence(new AbsenceRequest(startDay.getValue(), endDay.getValue(), AbsenceType.VACATION));
    }

    private void saveSick() {
        absenceService.saveAbsence(new AbsenceRequest(startDay.getValue(), endDay.getValue(), AbsenceType.SICK));
    }

    private void deleteWorkingTime() {
        timeEntryService.deleteEntry(inputsToTimeEntryDto());
    }

    private void deleteVacation() {
        absenceService.deleteAbsence(new AbsenceRequest(startDay.getValue(), endDay.getValue(), AbsenceType.VACATION));
    }

    private void deleteSick() {
        absenceService.deleteAbsence(new AbsenceRequest(startDay.getValue(), endDay.getValue(), AbsenceType.SICK));
    }

    private TimeEntryDTO inputsToTimeEntryDto() {
        TimeEntryDTO timeEntryDto = new TimeEntryDTO();
        timeEntryDto.setDate(Optional.of(startDay).map(DatePicker::getValue).map(date -> date.format(DateTimeFormatter.ISO_LOCAL_DATE.localizedBy(Locale.UK))).orElse(null));
        timeEntryDto.setStartTime(Optional.of(start).map(TimePicker::getValue).map(time -> time.format(DateTimeFormatter.ISO_TIME.localizedBy(Locale.UK))).orElse(null));
        timeEntryDto.setEndTime(Optional.of(end).map(TimePicker::getValue).map(time -> time.format(DateTimeFormatter.ISO_TIME.localizedBy(Locale.UK))).orElse(null));
        timeEntryDto.setBreakMinutes(Optional.of(breakMinutes).map(IntegerField::getValue).orElse(null));
        return timeEntryDto;
    }
}