package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class YearService {

    private final TimeEntryRepository timeEntryRepository;
    private final TimeEntryService timeEntryService;
    private final AbsenceService absenceService;
    private final UserRepository userRepository;


    public List<DaySummaryDTO> getYear(int year) {
        User user = userRepository.findById("1").orElseThrow();

        LocalDate yearStart = Year.of(year).atMonth(1).atDay(1);

        LocalDate yearEnd = yearStart.with(TemporalAdjusters.lastDayOfYear());

        List<TimeEntry> entries =
                timeEntryRepository.findByUserAndDateBetween(user, yearStart, yearEnd);

        List<Absence> absences =
                absenceService.getAbsences(user, yearStart, yearEnd);

        List<DaySummaryDTO> result = new ArrayList<>();

        int dailyTargetMinutes = (user.getWeeklyTargetHours() * 60) / 5;

        for (int i = 0; i < yearEnd.getDayOfYear(); i++) {
            LocalDate day = yearStart.plusDays(i);

            TimeEntry entry = entries.stream()
                    .filter(e -> e.getDate().equals(day))
                    .findFirst()
                    .orElse(null);

            Absence absence = absences.stream()
                    .filter(a -> !day.isBefore(a.getDate()) && !day.isAfter(a.getDate()))
                    .findFirst()
                    .orElse(null);

            int workedMinutes = entry != null
                    ? timeEntryService.calculateWorkedMinutes(entry)
                    : 0;

            int balance = workedMinutes - dailyTargetMinutes;
            AbsenceType dayType = null;

            if (absence != null) {
                dayType = absence.getType();
                workedMinutes = 0;
                balance = 0;
            } else if (isWeekend(day)) {
                workedMinutes = 0;
                balance = 0;
            }

            result.add(new DaySummaryDTO(
                    day,
                    workedMinutes > 0 ? workedMinutes : null,
                    balance,
                    dayType
            ));
        }

        return result;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}

