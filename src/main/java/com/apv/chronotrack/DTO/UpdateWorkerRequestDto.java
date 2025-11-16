package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateWorkerRequestDto {
    @NotBlank(message = "El nombre completo no puede estar vacío.")
    private String fullName;

    @NotNull(message = "La tarifa por hora no puede ser nula.")
    @DecimalMin(value = "0.0", inclusive = false, message = "La tarifa por hora debe ser mayor que cero.")
    private BigDecimal hourlyRate;

    @NotNull(message = "El estado de actividad no puede ser nulo.")
    private boolean isActive;

    private Long workLocationId;
}
