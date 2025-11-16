package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClockInRequestDto {
    @NotNull(message = "La latitud es requerida.")
    private Double latitude;

    @NotNull(message = "La longitud es requerida.")
    private Double longitude;
}
