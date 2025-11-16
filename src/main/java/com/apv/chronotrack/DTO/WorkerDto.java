package com.apv.chronotrack.DTO;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class WorkerDto {
    private Long id;
    private String fullName;
    private String email;
    private BigDecimal hourlyRate;
    private boolean isActive;
    private Long workLocationId;
    private String workLocationName;
}