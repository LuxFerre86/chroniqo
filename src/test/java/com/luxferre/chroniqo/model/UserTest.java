package com.luxferre.chroniqo.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
}