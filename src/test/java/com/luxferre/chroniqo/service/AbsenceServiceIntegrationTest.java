package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.AbsenceRepository;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.AutoConfigureTestEntityManager;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@AutoConfigureTestEntityManager
@WithMockUser("test@gmail.com")
public class AbsenceServiceIntegrationTest {

    @Autowired
    private AbsenceService absenceService;
    @Autowired
    private AbsenceRepository absenceRepository;

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

    @ParameterizedTest
    @EnumSource(AbsenceType.class)
    public void getAbsence_today_absencePresent(AbsenceType absenceType) {
        entityManager.persist(createAbsence(LocalDate.now(), absenceType));

        Absence absence = absenceService.getAbsence(LocalDate.now());

        assertAbsence(absence, LocalDate.now(), absenceType);
    }

    @Test
    public void getAbsence_today_absenceNotPresent() {
        Absence absence = absenceService.getAbsence(LocalDate.now());

        assertThat(absence).isNull();
    }

    @Test
    public void getAbsences_today_absencePresent() {
        entityManager.persist(createAbsence(LocalDate.now(), AbsenceType.VACATION));

        List<Absence> absences = absenceService.getAbsences(LocalDate.now(), LocalDate.now());

        assertThat(absences.size()).isEqualTo(1);
        assertAbsence(absences.getFirst(), LocalDate.now(), AbsenceType.VACATION);
    }

    @Test
    public void getAbsences_today_absenceNotPresent() {
        List<Absence> absences = absenceService.getAbsences(LocalDate.now(), LocalDate.now());

        assertThat(absences).isNotNull().isEmpty();
    }

    @Test
    public void deleteAbsence_today_absencePresent() {
        entityManager.persist(createAbsence(LocalDate.now(), AbsenceType.VACATION));
        Absence absenceBefore = absenceRepository.findByUserAndDate(testUser, LocalDate.now());
        assertThat(absenceBefore).isNotNull();

        absenceService.deleteAbsence(LocalDate.now());

        Absence absenceAfter = absenceRepository.findByUserAndDate(testUser, LocalDate.now());
        assertThat(absenceAfter).isNull();
    }

    @Test
    public void deleteAbsence_today_absenceNotPresent() {
        Absence absenceBefore = absenceRepository.findByUserAndDate(testUser, LocalDate.now());
        assertThat(absenceBefore).isNull();

        absenceService.deleteAbsence(LocalDate.now());

        Absence absenceAfter = absenceRepository.findByUserAndDate(testUser, LocalDate.now());
        assertThat(absenceAfter).isNull();
    }

    @Test
    public void deleteAbsences_today_absencePresent() {
        entityManager.persist(createAbsence(LocalDate.now(), AbsenceType.VACATION));
        Absence absenceBefore = absenceRepository.findByUserAndDate(testUser, LocalDate.now());
        assertThat(absenceBefore).isNotNull();

        absenceService.deleteAbsences(LocalDate.now(), LocalDate.now());

        Absence absenceAfter = absenceRepository.findByUserAndDate(testUser, LocalDate.now());
        assertThat(absenceAfter).isNull();
    }

    @Test
    public void deleteAbsences_today_absenceNotPresent() {
        Absence absenceBefore = absenceRepository.findByUserAndDate(testUser, LocalDate.now());
        assertThat(absenceBefore).isNull();

        absenceService.deleteAbsences(LocalDate.now(), LocalDate.now());

        Absence absenceAfter = absenceRepository.findByUserAndDate(testUser, LocalDate.now());
        assertThat(absenceAfter).isNull();
    }

    @Test
    public void saveAbsence() {
        LocalDate currentWeeksStart = getCurrentWeeksStart();
        AbsenceRequest absenceRequest = new AbsenceRequest(currentWeeksStart, currentWeeksStart, AbsenceType.VACATION);

        absenceService.saveAbsence(absenceRequest);

        Absence absenceAfter = absenceRepository.findByUserAndDate(testUser, currentWeeksStart);

        assertThat(absenceAfter).isNotNull();
        assertThat(absenceAfter.getDate()).isEqualTo(currentWeeksStart);
        assertThat(absenceAfter.getType()).isEqualTo(AbsenceType.VACATION);
        assertThat(absenceAfter.getUser()).isEqualTo(testUser);
        assertThat(absenceAfter.getId()).isNotNull();
    }

    @Test
    public void saveAbsence_updateAbsence() {
        LocalDate currentWeeksStart = getCurrentWeeksStart();
        entityManager.persist(createAbsence(currentWeeksStart, AbsenceType.VACATION));
        AbsenceRequest absenceRequest = new AbsenceRequest(currentWeeksStart, currentWeeksStart, AbsenceType.SICK);
        Absence absenceBefore = absenceRepository.findByUserAndDate(testUser, currentWeeksStart);
        assertThat(absenceBefore).isNotNull();

        absenceService.saveAbsence(absenceRequest);

        Absence absenceAfter = absenceRepository.findByUserAndDate(testUser, currentWeeksStart);

        assertThat(absenceAfter).isNotNull();
        assertThat(absenceAfter.getDate()).isEqualTo(currentWeeksStart);
        assertThat(absenceAfter.getType()).isEqualTo(AbsenceType.SICK);
        assertThat(absenceAfter.getUser()).isEqualTo(testUser);
        assertThat(absenceAfter.getId()).isNotEqualTo(absenceBefore.getId()); // because delete + insert
    }

    @Test
    public void saveAbsence_absenceForOneWeek() {
        LocalDate weekStart = getCurrentWeeksStart();
        LocalDate weekEnd = getCurrentWeeksEnd();
        AbsenceRequest absenceRequest = new AbsenceRequest(weekStart, weekEnd, AbsenceType.VACATION);

        absenceService.saveAbsence(absenceRequest);

        List<Absence> absencesAfter = absenceRepository.findByUserAndDateBetween(testUser, weekStart, weekEnd);

        assertThat(absencesAfter).isNotNull().hasSize(5);
        IntStream.rangeClosed(0, 4).forEach(i -> {
            assertThat(absencesAfter.get(i).getDate()).isEqualTo(weekStart.plusDays(i));
            assertThat(absencesAfter.get(i).getType()).isEqualTo(AbsenceType.VACATION);
            assertThat(absencesAfter.get(i).getUser()).isEqualTo(testUser);
            assertThat(absencesAfter.get(i).getId()).isNotNull();
        });
    }

    private void assertAbsence(Absence absence, LocalDate date, AbsenceType expectedAbsenceType) {
        assertThat(absence).isNotNull();
        assertThat(absence.getDate()).isEqualTo(date);
        assertThat(absence.getType()).isEqualTo(expectedAbsenceType);
    }

    private Absence createAbsence(LocalDate date, AbsenceType absenceType) {
        Absence absence = new Absence();
        absence.setDate(date);
        absence.setType(absenceType);
        absence.setUser(testUser);
        return absence;
    }

    private LocalDate getCurrentWeeksStart() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.GERMANY);
        return today.with(weekFields.dayOfWeek(), 1);
    }

    private LocalDate getCurrentWeeksEnd() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.GERMANY);
        return today.with(weekFields.dayOfWeek(), 7);
    }
}
