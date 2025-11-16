package com.apv.chronotrack.DTO;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DashboardStatsDto {
    private long totalWorkers;
    private long activeWorkersToday;
    private double totalHoursThisWeek;
    private BigDecimal estimatedWeeklyPayroll;
}
