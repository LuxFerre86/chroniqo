package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.TimeEntryStatus;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.util.IsWeekendQuery;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.AutoConfigureTestEntityManager;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@AutoConfigureTestEntityManager
@WithMockUser("test@gmail.com")
public class SummaryServiceIntegrationTest {

    @Autowired
    private SummaryService summaryService;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = entityManager.find(User.class, "a0000000-0000-0000-0000-000000000001");
        UI ui = new UI();
        ui.setLocale(Locale.GERMANY);
        UI.setCurrent(ui);
    }

    @Test
    public void getToday_withPresentTimeEntry() {
        entityManager.persist(createTimeEntry(LocalDate.now()));

        DaySummaryDTO daySummaryDTO = summaryService.getToday();

        assertDaySummaryDTO(daySummaryDTO, LocalDate.now());
    }

    @Test
    public void getToday_withTimeEntryNotPresent() {
        LocalDate today = LocalDate.now();
        DaySummaryDTO daySummaryDTO = summaryService.getToday();

        assertThat(daySummaryDTO).isNotNull();
        assertThat(daySummaryDTO.date()).isEqualTo(LocalDate.now());
        assertThat(daySummaryDTO.workedMinutes()).isEqualTo(0);
        boolean isWeekend = today.query(new IsWeekendQuery());
        if (isWeekend) {
            assertThat(daySummaryDTO.targetMinutes()).isEqualTo(0);
            assertThat(daySummaryDTO.balanceMinutes()).isEqualTo(0);
        } else {
            assertThat(daySummaryDTO.targetMinutes()).isEqualTo(468);
            assertThat(daySummaryDTO.balanceMinutes()).isEqualTo(-468);
        }
    }

    @Test
    public void getCurrentWeek_withPresentTimeEntries() {
        List<TimeEntry> timeEntries = addCurrentWeekTestData();

        List<DaySummaryDTO> daySummaryDTOs = summaryService.getCurrentWeek();

        assertThat(daySummaryDTOs).isNotNull().hasSize(timeEntries.size());
        IntStream.range(0, timeEntries.size()).forEach(i -> assertDaySummaryDTO(daySummaryDTOs.get(i), timeEntries.get(i).getDate()));
    }

    @Test
    public void getSummary_currentYear() {
        int currentYear = LocalDate.now().getYear();
        LocalDate yearEnd = LocalDate.now().with(TemporalAdjusters.lastDayOfYear());
        List<TimeEntry> timeEntries = LocalDate.of(currentYear, 1, 1).datesUntil(yearEnd.plusDays(1)).map(this::createTimeEntry).toList();
        timeEntries.forEach(entityManager::persist);

        List<DaySummaryDTO> daySummaryDTOs = summaryService.getSummary(currentYear);

        assertThat(daySummaryDTOs).isNotNull().hasSize(timeEntries.size());
        IntStream.range(0, timeEntries.size()).forEach(i -> assertDaySummaryDTO(daySummaryDTOs.get(i), timeEntries.get(i).getDate()));
    }

    @Test
    public void getSummary_fromStartOfYearToIncludingToday() {
        int currentYear = LocalDate.now().getYear();
        List<TimeEntry> timeEntries = LocalDate.of(currentYear, 1, 1).datesUntil(LocalDate.now().plusDays(1)).map(this::createTimeEntry).toList();
        timeEntries.forEach(entityManager::persist);

        List<DaySummaryDTO> daySummaryDTOs = summaryService.getSummary(LocalDate.of(currentYear, 1, 1), LocalDate.now());

        assertThat(daySummaryDTOs).isNotNull().hasSize(timeEntries.size());
        IntStream.range(0, timeEntries.size()).forEach(i -> assertDaySummaryDTO(daySummaryDTOs.get(i), timeEntries.get(i).getDate()));
    }

    @Test
    public void getToday_startTimeFuture() {
        TimeEntry timeEntry = new TimeEntry();
        timeEntry.setDate(LocalDate.now());
        timeEntry.setStartTime(LocalTime.now().plusMinutes(15));
        timeEntry.setUser(testUser);
        timeEntry.setStatus(TimeEntryStatus.STARTED);
        entityManager.persist(timeEntry);

        DaySummaryDTO daySummaryDTO = summaryService.getToday();

        assertThat(daySummaryDTO).isNotNull();
        assertThat(daySummaryDTO.date()).isEqualTo(LocalDate.now());
        assertThat(daySummaryDTO.workedMinutes()).isEqualTo(0);
        boolean isWeekend = LocalDate.now().query(new IsWeekendQuery());
        if (isWeekend) {
            assertThat(daySummaryDTO.targetMinutes()).isEqualTo(0);
            assertThat(daySummaryDTO.balanceMinutes()).isEqualTo(0);
        } else {
            assertThat(daySummaryDTO.targetMinutes()).isEqualTo(468);
            assertThat(daySummaryDTO.balanceMinutes()).isEqualTo(-468);
        }
    }

    private void assertDaySummaryDTO(DaySummaryDTO daySummaryDTO, LocalDate date) {
        assertThat(daySummaryDTO).isNotNull();
        assertThat(daySummaryDTO.date()).isEqualTo(date);
        boolean isWeekend = date.query(new IsWeekendQuery());
        if (!daySummaryDTO.isWorkday() && AbsenceType.HOLIDAY.equals(daySummaryDTO.absenceType())) {
            assertThat(daySummaryDTO.workedMinutes()).isEqualTo(0);
            assertThat(daySummaryDTO.targetMinutes()).isEqualTo(0);
            assertThat(daySummaryDTO.balanceMinutes()).isEqualTo(0);
        } else if (isWeekend) {
            assertThat(daySummaryDTO.workedMinutes()).isEqualTo(560);
            assertThat(daySummaryDTO.targetMinutes()).isEqualTo(0);
            assertThat(daySummaryDTO.balanceMinutes()).isEqualTo(560);
        } else {
            assertThat(daySummaryDTO.workedMinutes()).isEqualTo(560);
            assertThat(daySummaryDTO.targetMinutes()).isEqualTo(468);
            assertThat(daySummaryDTO.balanceMinutes()).isEqualTo(92);
        }
    }

    private List<TimeEntry> addCurrentWeekTestData() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.GERMANY);
        LocalDate weekStart = today.with(weekFields.dayOfWeek(), 1);
        LocalDate weekEnd = today.with(weekFields.dayOfWeek(), 7);
        List<TimeEntry> list = weekStart.datesUntil(weekEnd.plusDays(1L)).map(this::createTimeEntry).toList();
        list.forEach(entityManager::persist);
        return list;
    }

    private TimeEntry createTimeEntry(LocalDate date) {
        TimeEntry timeEntry = new TimeEntry();
        timeEntry.setStatus(TimeEntryStatus.COMPLETED);
        timeEntry.setDate(date);
        timeEntry.setStartTime(LocalTime.of(7, 15));
        timeEntry.setEndTime(LocalTime.of(17, 45));
        timeEntry.setBreakMinutes(70);
        timeEntry.setUser(testUser);
        timeEntry.setCreatedAt(date.atTime(LocalTime.of(18, 0)));
        timeEntry.setCompletedAt(date.atTime(LocalTime.of(18, 0)));
        return timeEntry;
    }
}
