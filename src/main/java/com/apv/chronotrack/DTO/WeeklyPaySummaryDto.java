package com.apv.chronotrack.DTO;

import com.apv.chronotrack.models.TimesheetStatus;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.models.WorkWeek;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class WeeklyPaySummaryDto {
    private WorkWeekDto workWeek; // <-- De Entidad a DTO
    private Long workerId;        // <-- De Entidad a ID
    private String workerName;    // <-- Añadimos el nombre
    private double regularHours;
    private double overtimeHours;
    private double totalHours;
    private BigDecimal regularPay;
    private BigDecimal overtimePay;
    private BigDecimal totalPay;
    private TimesheetStatus status; // <-- AÑADIR
}
