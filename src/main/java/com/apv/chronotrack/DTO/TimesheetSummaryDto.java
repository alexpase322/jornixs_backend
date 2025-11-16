package com.apv.chronotrack.DTO;

import com.apv.chronotrack.models.TimesheetStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class TimesheetSummaryDto {
    private Long timesheetId;
    private Long workerId;
    private String workerName;
    private Long workWeekId;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private TimesheetStatus status;
    private String rejectionReason; // <-- CAMPO AÑADIDO
}