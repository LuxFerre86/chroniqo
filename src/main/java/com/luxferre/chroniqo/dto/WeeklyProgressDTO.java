package com.luxferre.chroniqo.dto;

/**
 * Aggregated weekly progress data used by the dashboard widgets.
 *
 * <p>{@code percentage} is the ratio of {@code workedMinutes} to
 * {@code targetMinutes}, clamped to [0, 100] before display.
 * {@code hasTarget} is {@code false} when the user has no weekly target
 * configured (i.e. {@code targetMinutes == 0}), allowing the UI to display
 * a placeholder instead of a meaningless 0 % progress bar.
 *
 * @author Luxferre86
 * @since 28.02.2026
 */
public record WeeklyProgressDTO(int workedMinutes, int targetMinutes, int percentage, boolean hasTarget) {
}