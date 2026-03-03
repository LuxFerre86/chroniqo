package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.TimeEntryStatus;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@AutoConfigureTestEntityManager
@WithMockUser("test@gmail.com")
public class TimeEntryServiceIntegrationTest {

    @Autowired
    private TimeEntryService timeEntryService;
    @Autowired
    private TimeEntryRepository timeEntryRepository;

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
    public void getTimeEntries_today() {
        entityManager.persist(createTimeEntry(LocalDate.now()));

        TimeEntryDTO timeEntryDTO = timeEntryService.getTimeEntry(LocalDate.now());

        assertTimeEntryDTO(timeEntryDTO, LocalDate.now());
    }

    @Test
    public void getTimeEntries_withTimeEntryNotPresent() {
        TimeEntryDTO timeEntryDTO = timeEntryService.getTimeEntry(LocalDate.now());

        assertThat(timeEntryDTO).isNull();
    }

    @Test
    public void getTimeEntries_fromStartOfYearToIncludingToday() {
        int currentYear = LocalDate.now().getYear();
        List<TimeEntry> timeEntries = LocalDate.of(currentYear, 1, 1).datesUntil(LocalDate.now().plusDays(1)).map(this::createTimeEntry).toList();
        timeEntries.forEach(entityManager::persist);

        List<TimeEntryDTO> timeEntryDTOs = timeEntryService.getTimeEntries(LocalDate.of(currentYear, 1, 1), LocalDate.now());

        assertThat(timeEntryDTOs).isNotNull().hasSize(timeEntries.size());
        IntStream.range(0, timeEntries.size()).forEach(i -> assertTimeEntryDTO(timeEntryDTOs.get(i), timeEntries.get(i).getDate()));
    }

    @Test
    public void saveEntry() {
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO(LocalDate.now(), LocalTime.of(7, 15), LocalTime.of(17, 45), 70);

        timeEntryService.saveEntry(timeEntryDTO);

        TimeEntry timeEntry = timeEntryRepository.findByUserAndDate(testUser, LocalDate.now());

        assertThat(timeEntry).isNotNull();
        assertThat(timeEntry.getDate()).isEqualTo(LocalDate.now());
        assertThat(timeEntry.getStartTime()).isEqualTo(LocalTime.of(7, 15));
        assertThat(timeEntry.getEndTime()).isEqualTo(LocalTime.of(17, 45));
        assertThat(timeEntry.getBreakMinutes()).isEqualTo(70);
        assertThat(timeEntry.getStatus()).isEqualTo(TimeEntryStatus.COMPLETED);
        assertThat(timeEntry.getCreatedAt()).isNotNull();
        assertThat(timeEntry.getCompletedAt()).isNotNull();
    }

    @Test
    public void saveEntry_update() {
        entityManager.persist(createTimeEntry(LocalDate.now()));
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO(LocalDate.now(), LocalTime.of(7, 15), LocalTime.of(18, 50), 70);

        timeEntryService.saveEntry(timeEntryDTO);

        TimeEntry timeEntry = timeEntryRepository.findByUserAndDate(testUser, LocalDate.now());

        assertThat(timeEntry).isNotNull();
        assertThat(timeEntry.getDate()).isEqualTo(LocalDate.now());
        assertThat(timeEntry.getStartTime()).isEqualTo(LocalTime.of(7, 15));
        assertThat(timeEntry.getEndTime()).isEqualTo(LocalTime.of(18, 50));
        assertThat(timeEntry.getBreakMinutes()).isEqualTo(70);
        assertThat(timeEntry.getStatus()).isEqualTo(TimeEntryStatus.COMPLETED);
        assertThat(timeEntry.getCreatedAt()).isNotNull();
        assertThat(timeEntry.getCompletedAt()).isNotNull();
    }

    @Test
    public void saveEntry_statusStated() {
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO(LocalDate.now(), LocalTime.of(7, 15), null, 70);

        timeEntryService.saveEntry(timeEntryDTO);

        TimeEntry timeEntry = timeEntryRepository.findByUserAndDate(testUser, LocalDate.now());

        assertThat(timeEntry).isNotNull();
        assertThat(timeEntry.getDate()).isEqualTo(LocalDate.now());
        assertThat(timeEntry.getStartTime()).isEqualTo(LocalTime.of(7, 15));
        assertThat(timeEntry.getEndTime()).isNull();
        assertThat(timeEntry.getBreakMinutes()).isEqualTo(70);
        assertThat(timeEntry.getStatus()).isEqualTo(TimeEntryStatus.STARTED);
        assertThat(timeEntry.getCreatedAt()).isNotNull();
        assertThat(timeEntry.getCompletedAt()).isNull();
    }

    @Test
    public void deleteEntry() {
        TimeEntry timeEntry = createTimeEntry(LocalDate.now());
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO(timeEntry.getDate(), null, null, null);
        entityManager.persist(timeEntry);

        timeEntryService.deleteEntry(timeEntryDTO);

        TimeEntry timeEntryAfter = timeEntryRepository.findByUserAndDate(testUser, LocalDate.now());

        assertThat(timeEntryAfter).isNull();
    }

    private void assertTimeEntryDTO(TimeEntryDTO timeEntryDTO, LocalDate date) {
        assertThat(timeEntryDTO).isNotNull();
        assertThat(timeEntryDTO.getDate()).isEqualTo(date);
        assertThat(timeEntryDTO.getStartTime()).isEqualTo(LocalTime.of(7, 15));
        assertThat(timeEntryDTO.getEndTime()).isEqualTo(LocalTime.of(17, 45));
        assertThat(timeEntryDTO.getBreakMinutes()).isEqualTo(70);
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
