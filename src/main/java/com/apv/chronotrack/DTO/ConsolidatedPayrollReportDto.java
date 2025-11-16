package com.apv.chronotrack.DTO;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ConsolidatedPayrollReportDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<ConsolidatedPayrollEntryDto> entries;
    private BigDecimal grandTotalPay;
}
