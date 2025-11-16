package com.apv.chronotrack.DTO;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class WorkWeekDto {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
}