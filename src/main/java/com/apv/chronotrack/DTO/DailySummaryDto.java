package com.apv.chronotrack.DTO;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class DailySummaryDto {
    private LocalDate date;
    private String workLocationName;
    private LocalTime clockInTime;
    private LocalTime startLunchTime;
    private LocalTime endLunchTime;
    private LocalTime clockOutTime;
    private double totalHours;
    private BigDecimal dailyRate; // La tarifa de ese día
    private BigDecimal totalPay;

    private Long clockInLogId;
    private Long startLunchLogId;
    private Long endLunchLogId;
    private Long clockOutLogId;
}