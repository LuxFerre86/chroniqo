package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.TimeEntryStatus;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.util.LoggingTestUtils;
import ch.qos.logback.classic.Level;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.AfterEach;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private LoggingTestUtils logs;

    @BeforeEach
    public void setup() {
        testUser = entityManager.find(User.class, "a0000000-0000-0000-0000-000000000001");
        UI ui = new UI();
        ui.setLocale(Locale.GERMANY);
        UI.setCurrent(ui);
        logs = LoggingTestUtils.captureLogsFor(TimeEntryService.class);
    }

    @AfterEach
    public void tearDown() {
        logs.stop();
    }

    @Test
    public void getTimeEntries_today() {
        entityManager.persist(createTimeEntry(LocalDate.now()));

        List<TimeEntryDTO> timeEntryDTOs = timeEntryService.getTimeEntries(LocalDate.now());

        assertThat(timeEntryDTOs).hasSize(1);
        assertTimeEntryDTO(timeEntryDTOs.getFirst(), LocalDate.now());
        logs.assertContains(Level.INFO, "Retrieving time entries between");
    }

    @Test
    public void getTimeEntries_withTimeEntryNotPresent() {
        List<TimeEntryDTO> timeEntryDTOs = timeEntryService.getTimeEntries(LocalDate.now());

        assertThat(timeEntryDTOs).isEmpty();
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
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO(LocalDate.now(), LocalTime.of(7, 15), LocalTime.of(17, 45), 70, null);

        timeEntryService.saveEntry(timeEntryDTO);

        TimeEntry timeEntry = getSingleEntryForDate(LocalDate.now());

        assertThat(timeEntry).isNotNull();
        assertThat(timeEntry.getDate()).isEqualTo(LocalDate.now());
        assertThat(timeEntry.getStartTime()).isEqualTo(LocalTime.of(7, 15));
        assertThat(timeEntry.getEndTime()).isEqualTo(LocalTime.of(17, 45));
        assertThat(timeEntry.getBreakMinutes()).isEqualTo(70);
        assertThat(timeEntry.getStatus()).isEqualTo(TimeEntryStatus.COMPLETED);
        assertThat(timeEntry.getCreatedAt()).isNotNull();
        assertThat(timeEntry.getCompletedAt()).isNotNull();
        assertThat(timeEntry.getNotes()).isNull();

        logs.assertContains(Level.INFO, "Time entry saved");
    }

    @Test
    public void saveEntry_withNotes() {
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO(LocalDate.now(), LocalTime.of(7, 15), LocalTime.of(17, 45), 70, "Client meeting in the afternoon");

        timeEntryService.saveEntry(timeEntryDTO);

        TimeEntry timeEntry = getSingleEntryForDate(LocalDate.now());

        assertThat(timeEntry).isNotNull();
        assertThat(timeEntry.getNotes()).isEqualTo("Client meeting in the afternoon");
    }

    @Test
    public void saveEntry_update_notesAreUpdated() {
        TimeEntry existing = createTimeEntry(LocalDate.now());
        entityManager.persist(existing);
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO(LocalDate.now(), LocalTime.of(7, 15), LocalTime.of(18, 50), 70, "Updated note");
        timeEntryDTO.setId(existing.getId());

        timeEntryService.saveEntry(timeEntryDTO);

        TimeEntry timeEntry = getSingleEntryForDate(LocalDate.now());
        assertThat(timeEntry.getNotes()).isEqualTo("Updated note");
    }

    @Test
    public void saveEntry_update_notesAreClearedWhenNull() {
        TimeEntry existing = createTimeEntry(LocalDate.now());
        existing.setNotes("Old note");
        entityManager.persist(existing);
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO(LocalDate.now(), LocalTime.of(7, 15), LocalTime.of(18, 50), 70, null);
        timeEntryDTO.setId(existing.getId());

        timeEntryService.saveEntry(timeEntryDTO);

        TimeEntry timeEntry = getSingleEntryForDate(LocalDate.now());
        assertThat(timeEntry.getNotes()).isNull();
    }

    @Test
    public void saveEntry_update() {
        TimeEntry existing = createTimeEntry(LocalDate.now());
        entityManager.persist(existing);
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO(LocalDate.now(), LocalTime.of(7, 15), LocalTime.of(18, 50), 70, null);
        timeEntryDTO.setId(existing.getId());

        timeEntryService.saveEntry(timeEntryDTO);

        TimeEntry timeEntry = getSingleEntryForDate(LocalDate.now());

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
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO(LocalDate.now(), LocalTime.of(7, 15), null, 70, null);

        timeEntryService.saveEntry(timeEntryDTO);

        TimeEntry timeEntry = getSingleEntryForDate(LocalDate.now());

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
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO(timeEntry.getDate(), null, null, null, null);
        entityManager.persist(timeEntry);
        timeEntryDTO.setId(timeEntry.getId());

        timeEntryService.deleteEntry(timeEntryDTO);

        List<TimeEntry> timeEntryAfter = getEntriesForDate(LocalDate.now());

        assertThat(timeEntryAfter).isEmpty();
        logs.assertContains(Level.INFO, "Deleting time entry");
    }

    @Test
    public void saveEntry_multipleEntriesPerDay_arePersisted() {
        LocalDate date = LocalDate.now().minusDays(1);
        TimeEntryDTO morning = new TimeEntryDTO(date, LocalTime.of(8, 0), LocalTime.of(12, 0), 15, "Morning");
        TimeEntryDTO afternoon = new TimeEntryDTO(date, LocalTime.of(12, 0), LocalTime.of(16, 30), 30, "Afternoon");

        timeEntryService.saveEntry(morning);
        timeEntryService.saveEntry(afternoon);

        List<TimeEntry> entries = getEntriesForDate(date);
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).getStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(entries.get(1).getStartTime()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    public void saveEntry_overlappingEntries_areRejected() {
        LocalDate date = LocalDate.now().minusDays(2);
        TimeEntryDTO existing = new TimeEntryDTO(date, LocalTime.of(9, 0), LocalTime.of(12, 0), 15, null);
        TimeEntryDTO overlapping = new TimeEntryDTO(date, LocalTime.of(11, 30), LocalTime.of(13, 0), 15, null);

        timeEntryService.saveEntry(existing);

        assertThatThrownBy(() -> timeEntryService.saveEntry(overlapping))
                .isInstanceOf(TimeEntryValidationException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    public void getTimeEntry_notesAreMappedToDTO() {
        TimeEntry timeEntry = createTimeEntry(LocalDate.now());
        timeEntry.setNotes("Important day");
        entityManager.persist(timeEntry);

        TimeEntryDTO dto = timeEntryService.getTimeEntries(LocalDate.now()).getFirst();

        assertThat(dto.getNotes()).isEqualTo("Important day");
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

    private TimeEntry getSingleEntryForDate(LocalDate date) {
        return getEntriesForDate(date).stream().findFirst().orElse(null);
    }

    private List<TimeEntry> getEntriesForDate(LocalDate date) {
        return timeEntryRepository.findByUserAndDateOrderByStartTimeAsc(testUser, date);
    }
}
