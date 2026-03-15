package com.luxferre.chroniqo.dto;

import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.service.SummaryService;

import java.time.LocalDate;

/**
 * Read-only summary of a single calendar day as computed by
 * {@link SummaryService}.
 *
 * <p>{@code workedMinutes} reflects net working time after subtracting break
 * minutes. {@code balanceMinutes} is positive when the user worked more than
 * their daily target and negative when they worked less. On weekend days the
 * target is always zero, so any time worked counts as positive balance.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
public record DaySummaryDTO(
        LocalDate date,
        boolean isWorkday,
        int workedMinutes,
        int targetMinutes,
        int balanceMinutes,
        AbsenceType absenceType
) {
}