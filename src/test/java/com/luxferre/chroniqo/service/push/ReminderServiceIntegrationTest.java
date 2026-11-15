package com.luxferre.chroniqo.service.push;

import com.luxferre.chroniqo.api.dto.ReminderSettingsDto;
import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.TimeEntryStatus;
import com.luxferre.chroniqo.repository.PushSubscriptionRepository;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.service.AbsenceService;
import com.luxferre.chroniqo.service.CustomHolidayService;
import com.luxferre.chroniqo.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test user: Mon-Fri, Hamburg. 2031-03-03 is a Monday, 2031-05-01 a holiday.
 */
@SpringBootTest
@Transactional
@WithMockUser("test@gmail.com")
class ReminderServiceIntegrationTest {

    private static final LocalDate MONDAY = LocalDate.of(2031, 3, 3);
    private static final LocalDate SATURDAY = LocalDate.of(2031, 3, 8);
    private static final LocalDate HOLIDAY = LocalDate.of(2031, 5, 1);
    private static final String ENDPOINT = "https://push.example/1";

    @MockitoBean
    private PushSender pushSender;
    @Autowired
    private ReminderService reminderService;
    @Autowired
    private PushSubscriptionService subscriptionService;
    @Autowired
    private PushSubscriptionRepository subscriptionRepository;
    @Autowired
    private TimeEntryRepository timeEntryRepository;
    @Autowired
    private CustomHolidayService customHolidayService;
    @Autowired
    private AbsenceService absenceService;
    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        when(pushSender.isEnabled()).thenReturn(true);
        when(pushSender.send(any(), any())).thenReturn(PushSender.Result.SENT);
        subscriptionService.subscribe(ENDPOINT, "p256dh", "auth");
    }

    private static LocalDateTime at(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date, LocalTime.of(hour, minute));
    }

    @Test
    void startReminder_sentOnceOnWorkingDayWithoutEntries() {
        reminderService.process(at(MONDAY, 9, 0));
        reminderService.process(at(MONDAY, 9, 1));

        verify(pushSender, times(1)).send(any(), any());
    }

    @Test
    void startReminder_notBeforeConfiguredTimeOrAfterWindow() {
        reminderService.process(at(MONDAY, 8, 59));
        reminderService.process(at(MONDAY, 9 + ReminderService.START_WINDOW_HOURS, 0));

        verify(pushSender, never()).send(any(), any());
    }

    @Test
    void startReminder_respectsCustomTimeAndDisabledSwitch() {
        subscriptionService.updateSettings(new ReminderSettingsDto(true, LocalTime.of(7, 30), true, 10));
        reminderService.process(at(MONDAY, 7, 30));
        verify(pushSender, times(1)).send(any(), any());

        subscriptionService.updateSettings(new ReminderSettingsDto(false, LocalTime.of(7, 30), true, 10));
        reminderService.process(at(MONDAY.plusDays(1), 7, 30));
        verify(pushSender, times(1)).send(any(), any());
    }

    @Test
    void startReminder_notOnWeekendOrHoliday() {
        reminderService.process(at(SATURDAY, 9, 30));
        reminderService.process(at(HOLIDAY, 9, 30));

        verify(pushSender, never()).send(any(), any());
    }

    @Test
    void startReminder_notOnCustomDayOff() {
        customHolidayService.create("Bridge day", MONDAY, MONDAY, false);

        reminderService.process(at(MONDAY, 9, 30));

        verify(pushSender, never()).send(any(), any());
    }

    @Test
    void startReminder_notOnAbsenceDay() {
        absenceService.saveAbsence(new AbsenceRequest(MONDAY, MONDAY, AbsenceType.SICK));

        reminderService.process(at(MONDAY, 9, 30));

        verify(pushSender, never()).send(any(), any());
    }

    @Test
    void startReminder_notWhenTimeAlreadyLogged() {
        timeEntryRepository.save(entry(MONDAY, TimeEntryStatus.COMPLETED));

        reminderService.process(at(MONDAY, 9, 30));

        verify(pushSender, never()).send(any(), any());
    }

    @Test
    void endReminder_sentOnceForOpenEntryAfterConfiguredDuration() {
        timeEntryRepository.save(entry(MONDAY, TimeEntryStatus.STARTED));

        reminderService.process(at(MONDAY, 17, 59));
        verify(pushSender, never()).send(any(), any());

        reminderService.process(at(MONDAY, 18, 0));
        reminderService.process(at(MONDAY, 18, 1));
        verify(pushSender, times(1)).send(any(), any());
    }

    @Test
    void endReminder_usesConfiguredHours() {
        timeEntryRepository.save(entry(MONDAY, TimeEntryStatus.STARTED));
        subscriptionService.updateSettings(new ReminderSettingsDto(false, LocalTime.of(9, 0), true, 4));

        reminderService.process(at(MONDAY, 12, 0));

        verify(pushSender, times(1)).send(any(), any());
    }

    @Test
    void endReminder_canBeDisabled() {
        timeEntryRepository.save(entry(MONDAY, TimeEntryStatus.STARTED));
        subscriptionService.updateSettings(new ReminderSettingsDto(false, LocalTime.of(9, 0), false, 4));

        reminderService.process(at(MONDAY, 23, 0));

        verify(pushSender, never()).send(any(), any());
    }

    @Test
    void goneSubscription_isDeleted() {
        when(pushSender.send(any(), any())).thenReturn(PushSender.Result.GONE);

        reminderService.process(at(MONDAY, 9, 30));

        assertThat(subscriptionRepository.findByEndpoint(ENDPOINT)).isEmpty();
    }

    @Test
    void disabledSender_doesNothing() {
        when(pushSender.isEnabled()).thenReturn(false);

        reminderService.process(at(MONDAY, 9, 30));

        verify(pushSender, never()).send(any(), any());
    }

    @Test
    void settings_defaultsAndValidation() {
        ReminderSettingsDto defaults = subscriptionService.getSettings();
        assertThat(defaults.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(defaults.endAfterHours()).isEqualTo(10);

        assertThatThrownBy(() -> subscriptionService.updateSettings(
                new ReminderSettingsDto(true, LocalTime.of(9, 0), true, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void subscribe_sameEndpointTwice_keepsOneEntryAndUnsubscribeRemovesIt() {
        subscriptionService.subscribe(ENDPOINT, "new", "new");
        assertThat(subscriptionRepository.findByEndpoint(ENDPOINT).orElseThrow().getP256dh()).isEqualTo("new");

        subscriptionService.unsubscribe(ENDPOINT);
        assertThat(subscriptionRepository.findByEndpoint(ENDPOINT)).isEmpty();
    }

    private TimeEntry entry(LocalDate date, TimeEntryStatus status) {
        TimeEntry entry = new TimeEntry();
        entry.setUser(userService.getCurrentUser());
        entry.setDate(date);
        entry.setStartTime(LocalTime.of(8, 0));
        entry.setStatus(status);
        if (status == TimeEntryStatus.COMPLETED) {
            entry.setEndTime(LocalTime.of(9, 0));
        }
        return entry;
    }
}
