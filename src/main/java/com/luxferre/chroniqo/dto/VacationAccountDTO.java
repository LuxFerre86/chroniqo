package com.luxferre.chroniqo.dto;

/**
 * Vacation account of one calendar year in whole days.
 *
 * @param year        the calendar year
 * @param entitlement the annual entitlement
 * @param taken       vacation days up to and including today
 * @param planned     vacation days after today
 * @param remaining   entitlement minus taken and planned; negative when overbooked
 */
public record VacationAccountDTO(int year, int entitlement, int taken, int planned, int remaining) {
}
