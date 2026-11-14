package com.luxferre.chroniqo.api.dto;

import com.luxferre.chroniqo.dto.WeeklyProgressDTO;

import java.util.List;

/**
 * Everything the dashboard needs in a single response.
 *
 * @param today                today's summary
 * @param week                 summaries of the current week
 * @param currentBalanceMinutes running balance from the start of the year until today
 * @param weeklyProgress       progress towards the weekly target
 */
public record DashboardResponse(DaySummaryResponse today,
                                List<DaySummaryResponse> week,
                                int currentBalanceMinutes,
                                WeeklyProgressDTO weeklyProgress) {
}
