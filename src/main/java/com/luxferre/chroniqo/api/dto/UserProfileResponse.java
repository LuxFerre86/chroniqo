package com.luxferre.chroniqo.api.dto;

import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.util.WeekStart;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Profile of the authenticated user.
 */
public record UserProfileResponse(String email,
                                  String firstName,
                                  String lastName,
                                  BigDecimal weeklyTargetHours,
                                  List<DayOfWeek> workingDays,
                                  DayOfWeek firstDayOfWeek,
                                  String countryCode,
                                  String subdivisionCode,
                                  LocalDateTime createdAt,
                                  LocalDateTime lastLoginAt) {

    /**
     * Maps a {@link User} entity to its API representation.
     *
     * @param user the user entity
     * @return the profile response
     */
    public static UserProfileResponse of(User user) {
        return new UserProfileResponse(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getWeeklyTargetHours(),
                List.copyOf(user.getWorkingDaysOrDefault()),
                WeekStart.forCountry(user.getCountryCode()),
                user.getCountryCode(),
                user.getSubdivisionCode(),
                user.getCreatedAt(),
                user.getLastLoginAt());
    }
}
