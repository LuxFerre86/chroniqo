package com.luxferre.chroniqo.frontend;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.service.AbsenceService;
import com.luxferre.chroniqo.service.TimeEntryService;
import com.vaadin.flow.component.ClickEvent;
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
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;


@UIScope
@Component
public class TimeEntryDialog extends Dialog {

    private DatePicker startDay = new DatePicker(LocalDate.now());
    private DatePicker endDay = new DatePicker(LocalDate.now());
    private TimePicker start = new TimePicker("Start Time");
    private TimePicker end = new TimePicker("End Time");
    private IntegerField breakMinutes = new IntegerField("Break");

    private Tabs tabs = new Tabs();
    private Tab workingTimeTab = new Tab("Working Time");
    private Tab sickTab = new Tab("Sick");
    private Tab vacationTab = new Tab("Vacation");
    private VerticalLayout content = new VerticalLayout();

    private Button saveButton = new Button("Save");
    private Button deleteButton = new Button("Delete");

    private final TimeEntryService timeEntryService;
    private final AbsenceService absenceService;

    private Binder<TimePicker> timePickerBinder = new Binder<>();


    public TimeEntryDialog(TimeEntryService timeEntryService, AbsenceService absenceService) {
        this.timeEntryService = timeEntryService;
        this.absenceService = absenceService;
        renderDialog();
        renderWorkingTime();
        timePickerBinder.forField(start).withValidator(localTime -> null != localTime,"Start Time is mandatory!").bind(TimePicker::getValue,TimePicker::setValue);
    }

    void renderDialog() {
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

        breakMinutes.setValue(30);
        breakMinutes.setStepButtonsVisible(true);
        breakMinutes.setStep(5);
        breakMinutes.setMin(30);
        breakMinutes.setMax(480);

        Button cancelButton = new Button("Cancel", e -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.AURA_TERTIARY);

        getHeader().add(tabs);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.add(startDay, endDay, start, end, breakMinutes);


        saveButton.addClickListener(this::save);
        deleteButton.addThemeVariants(ButtonVariant.AURA_DANGER);
        deleteButton.addClickListener(this::delete);

        add(content);
        getFooter().add(saveButton, deleteButton, cancelButton);
    }

    public void renderWorkingTime() {
        startDay.setVisible(true);
        startDay.setLabel("Day");
        endDay.setVisible(false);
        start.setVisible(true);
        end.setVisible(true);
        breakMinutes.setVisible(true);
    }

    public void renderSick() {
        startDay.setVisible(true);
        startDay.setLabel("Start");
        endDay.setVisible(true);
        endDay.setLabel("End");
        start.setVisible(false);
        end.setVisible(false);
        breakMinutes.setVisible(false);
    }

    public void renderVacation() {
        startDay.setVisible(true);
        startDay.setLabel("Start");
        endDay.setVisible(true);
        endDay.setLabel("End");
        start.setVisible(false);
        end.setVisible(false);
        breakMinutes.setVisible(false);
    }

    public void open(LocalDate date) {
        reset();
        startDay.setValue(date);
        endDay.setValue(date);
        TimeEntryDTO entry = timeEntryService.getEntry(date);
        tabs.setSelectedTab(workingTimeTab);
        if (null != entry) {
            start.setValue(LocalTime.parse(entry.getStartTime()));
            end.setValue(LocalTime.parse(entry.getEndTime()));
            breakMinutes.setValue(entry.getBreakMinutes());
        } else {
            Absence absence = absenceService.getAbsence(startDay.getValue());
            if (null != absence) {
                if (absence.getType() == AbsenceType.SICK) {
                    tabs.setSelectedTab(sickTab);
                } else if (absence.getType() == AbsenceType.VACATION) {
                    tabs.setSelectedTab(vacationTab);
                }
            } else {
              breakMinutes.setValue(30);
            }
        }
        open();
    }

    private void reset(){
        startDay.setValue(null);
        endDay.setValue(null);
        start.setValue(null);
        end.setValue(null);
        breakMinutes.setValue(null);
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
        TimeEntryDTO timeEntryDto = new TimeEntryDTO();
        timeEntryDto.setDate(startDay.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE));
        timeEntryDto.setStartTime(start.getValue().format(DateTimeFormatter.ISO_TIME));
        timeEntryDto.setEndTime(end.getValue().format(DateTimeFormatter.ISO_TIME));
        timeEntryDto.setBreakMinutes(breakMinutes.getValue());
        timeEntryService.saveEntry(timeEntryDto);
    }

    private void saveVacation() {
        absenceService.saveAbsence(new AbsenceRequest(startDay.getValue(), endDay.getValue(), AbsenceType.VACATION));
    }

    private void saveSick() {
        absenceService.saveAbsence(new AbsenceRequest(startDay.getValue(), endDay.getValue(), AbsenceType.SICK));
    }

    private void deleteWorkingTime() {
        TimeEntryDTO timeEntryDto = new TimeEntryDTO();
        timeEntryDto.setDate(startDay.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE));
        timeEntryDto.setStartTime(start.getValue().format(DateTimeFormatter.ISO_TIME));
        timeEntryDto.setEndTime(end.getValue().format(DateTimeFormatter.ISO_TIME));
        timeEntryDto.setBreakMinutes(breakMinutes.getValue());
        timeEntryService.deleteEntry(timeEntryDto);
    }

    private void deleteVacation() {
        absenceService.deleteAbsence(new AbsenceRequest(startDay.getValue(), endDay.getValue(), AbsenceType.VACATION));
    }

    private void deleteSick() {
        absenceService.deleteAbsence(new AbsenceRequest(startDay.getValue(), endDay.getValue(), AbsenceType.SICK));
    }

}
