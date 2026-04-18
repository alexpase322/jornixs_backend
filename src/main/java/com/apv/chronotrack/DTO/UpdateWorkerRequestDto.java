package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateWorkerRequestDto {
    @NotBlank(message = "Full name cannot be empty.")
    private String fullName;

    @NotNull(message = "Hourly rate cannot be null.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Hourly rate must be greater than zero.")
    private BigDecimal hourlyRate;

    @NotNull(message = "Active status cannot be null.")
    private boolean isActive;

    private Long workLocationId;
}
