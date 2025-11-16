package com.apv.chronotrack.DTO;

import lombok.Builder;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DetailedPayrollReportDto {
    private Long workerId;
    private String workerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<WeeklyPaySummaryDto> weeklySummaries;
    private Map<Long, List<DailySummaryDto>> dailySummariesByWeek;
    private double grandTotalHours;
    private double grandTotalRegularHours;
    private double grandTotalOvertimeHours;
    private BigDecimal grandTotalPay;
    @Builder.Default
    private Map<Long, List<TimeLogDTO>> timeLogsByWeek = new HashMap<>();
    private String companyName;
    private String companyAddress;
    private String companyPhoneNumber;
}
