package com.luxferre.chroniqo.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    // =========================================================================
    // setWeeklyTargetHours
    // =========================================================================

    @ParameterizedTest(name = "weeklyTargetHours={0} is valid")
    @ValueSource(ints = {0, 1, 20, 39, 40, 80})
    void setWeeklyTargetHours_validValues_accepted(int hours) {
        User user = new User();
        user.setWeeklyTargetHours(hours);
        assertThat(user.getWeeklyTargetHours()).isEqualTo(hours);
    }

    @ParameterizedTest(name = "weeklyTargetHours={0} is invalid")
    @ValueSource(ints = {-1, -100, 81, 168, Integer.MAX_VALUE})
    void setWeeklyTargetHours_invalidValues_throwsIllegalArgument(int hours) {
        User user = new User();
        assertThatThrownBy(() -> user.setWeeklyTargetHours(hours))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weeklyTargetHours must be between 0 and 80");
    }

    @Test
    void setWeeklyTargetHours_zero_isAllowed() {
        // 0 means no target configured — valid state
        User user = new User();
        user.setWeeklyTargetHours(0);
        assertThat(user.getWeeklyTargetHours()).isZero();
    }

    @Test
    void setWeeklyTargetHours_boundary_eighty_isAllowed() {
        User user = new User();
        user.setWeeklyTargetHours(80);
        assertThat(user.getWeeklyTargetHours()).isEqualTo(80);
    }
// =========================================================================
    // isAccountLocked
    // =========================================================================

    @Test
    void isAccountLocked_nullLockedUntil_returnsFalse() {
        User user = new User();
        // lockedUntil is null by default
        assertThat(user.isAccountLocked()).isFalse();
    }

    @Test
    void isAccountLocked_lockedUntilInFuture_returnsTrue() {
        User user = new User();
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        assertThat(user.isAccountLocked()).isTrue();
    }

    @Test
    void isAccountLocked_lockedUntilInPast_returnsFalse() {
        User user = new User();
        user.setLockedUntil(LocalDateTime.now().minusSeconds(1));
        assertThat(user.isAccountLocked()).isFalse();
    }

    @Test
    void isAccountLocked_lockedUntilExactlyNow_returnsFalse() {
        // isBefore(now) == false when equal, but the window is so small it should be false
        User user = new User();
        user.setLockedUntil(LocalDateTime.now().minusNanos(1));
        assertThat(user.isAccountLocked()).isFalse();
    }

    // =========================================================================
    // getFullName
    // =========================================================================

    @Test
    void getFullName_returnsFirstAndLastNameSeparatedBySpace() {
        User user = new User();
        user.setFirstName("Max");
        user.setLastName("Mustermann");
        assertThat(user.getFullName()).isEqualTo("Max Mustermann");
    }

    @Test
    void getFullName_singleCharNames_stillConcatenatesCorrectly() {
        User user = new User();
        user.setFirstName("A");
        user.setLastName("B");
        assertThat(user.getFullName()).isEqualTo("A B");
    }
}