package com.luxferre.chroniqo.dto;

public record WeeklyProgressDTO(int workedMinutes, int targetMinutes, int percentage, boolean hasTarget) {
}
