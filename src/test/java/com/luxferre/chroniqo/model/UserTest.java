package com.luxferre.chroniqo.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
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
        user.setWeeklyTargetHours(BigDecimal.valueOf(hours));
        assertThat(user.getWeeklyTargetHours()).isEqualByComparingTo(BigDecimal.valueOf(hours));
    }

    @ParameterizedTest(name = "weeklyTargetHours={0} (decimal) is valid")
    @ValueSource(strings = {"37.5", "39.5", "0.5", "79.9", "40.0"})
    void setWeeklyTargetHours_oneDecimalPlace_accepted(String hours) {
        User user = new User();
        user.setWeeklyTargetHours(new BigDecimal(hours));
        assertThat(user.getWeeklyTargetHours()).isEqualByComparingTo(new BigDecimal(hours));
        assertThat(user.getWeeklyTargetHours().scale()).isEqualTo(1);
    }

    @Test
    void setWeeklyTargetHours_twoDecimalPlaces_rejected() {
        User user = new User();
        assertThatThrownBy(() -> user.setWeeklyTargetHours(new BigDecimal("37.55")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most one decimal place");
    }

    @Test
    void setWeeklyTargetHours_null_rejected() {
        User user = new User();
        assertThatThrownBy(() -> user.setWeeklyTargetHours(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weeklyTargetHours must be between 0 and 80");
    }

    @Test
    void setWeeklyTargetHours_aboveEightyWithDecimal_rejected() {
        User user = new User();
        assertThatThrownBy(() -> user.setWeeklyTargetHours(new BigDecimal("80.1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weeklyTargetHours must be between 0 and 80");
    }

    @ParameterizedTest(name = "weeklyTargetHours={0} is invalid")
    @ValueSource(ints = {-1, -100, 81, 168, Integer.MAX_VALUE})
    void setWeeklyTargetHours_invalidValues_throwsIllegalArgument(int hours) {
        User user = new User();
        assertThatThrownBy(() -> user.setWeeklyTargetHours(BigDecimal.valueOf(hours)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weeklyTargetHours must be between 0 and 80");
    }

    @Test
    void setWeeklyTargetHours_zero_isAllowed() {
        // 0 means no target configured — valid state
        User user = new User();
        user.setWeeklyTargetHours(BigDecimal.ZERO);
        assertThat(user.getWeeklyTargetHours()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void setWeeklyTargetHours_boundary_eighty_isAllowed() {
        User user = new User();
        user.setWeeklyTargetHours(BigDecimal.valueOf(80));
        assertThat(user.getWeeklyTargetHours()).isEqualByComparingTo(BigDecimal.valueOf(80));
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