package com.luxferre.chroniqo.service.user;

import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.TimeEntryStatus;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.AbsenceRepository;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@AutoConfigureTestEntityManager
@WithMockUser("test@gmail.com")
class UserServiceIntegrationTest {

    private static final String TEST_USER_ID = "a0000000-0000-0000-0000-000000000001";

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TimeEntryRepository timeEntryRepository;
    @Autowired
    private AbsenceRepository absenceRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = entityManager.find(User.class, TEST_USER_ID);
        testUser.setPasswordHash(passwordEncoder.encode("DeleteMe123!"));
        entityManager.flush();
    }

    @Test
    void deleteCurrentUserAccount_removesUserAndDependentRecords() {
        entityManager.persist(createTimeEntry(LocalDate.of(2026, 3, 3)));
        entityManager.persist(createAbsence(LocalDate.of(2026, 3, 4)));
        entityManager.flush();

        userService.deleteCurrentUserAccount("DeleteMe123!");
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(TEST_USER_ID)).isEmpty();
        assertThat(timeEntryRepository.count()).isZero();
        assertThat(absenceRepository.count()).isZero();
    }

    @Test
    void deleteCurrentUserAccount_wrongPassword_keepsUserAndDependentRecords() {
        entityManager.persist(createTimeEntry(LocalDate.of(2026, 3, 3)));
        entityManager.persist(createAbsence(LocalDate.of(2026, 3, 4)));
        entityManager.flush();

        assertThatThrownBy(() -> userService.deleteCurrentUserAccount("wrong-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incorrect");

        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(TEST_USER_ID)).isPresent();
        assertThat(timeEntryRepository.count()).isEqualTo(1);
        assertThat(absenceRepository.count()).isEqualTo(1);
    }

    private TimeEntry createTimeEntry(LocalDate date) {
        TimeEntry timeEntry = new TimeEntry();
        timeEntry.setUser(testUser);
        timeEntry.setDate(date);
        timeEntry.setStartTime(LocalTime.of(8, 0));
        timeEntry.setEndTime(LocalTime.of(16, 30));
        timeEntry.setBreakMinutes(30);
        timeEntry.setStatus(TimeEntryStatus.COMPLETED);
        return timeEntry;
    }

    private Absence createAbsence(LocalDate date) {
        Absence absence = new Absence();
        absence.setUser(testUser);
        absence.setDate(date);
        absence.setType(AbsenceType.VACATION);
        return absence;
    }
}

