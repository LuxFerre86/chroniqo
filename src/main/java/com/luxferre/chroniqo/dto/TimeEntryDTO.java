package com.luxferre.chroniqo.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class TimeEntryDTO {
    private String date;
    private String startTime;
    private String endTime;
    private Integer breakMinutes;
}
