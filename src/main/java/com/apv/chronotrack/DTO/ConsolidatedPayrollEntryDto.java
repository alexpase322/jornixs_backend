package com.apv.chronotrack.DTO;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ConsolidatedPayrollEntryDto {
    private Long workerId;
    private String workerName;
    private double totalRegularHours = 0.0;
    private double totalOvertimeHours = 0.0;
    private double totalHours = 0.0;
    private BigDecimal totalPay = BigDecimal.ZERO;

    // Constructor para inicializar con un trabajador
    public ConsolidatedPayrollEntryDto(Long workerId, String workerName) {
        this.workerId = workerId;
        this.workerName = workerName;
    }
}