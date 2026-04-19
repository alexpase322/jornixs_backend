package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClockInRequestDto {
    @NotNull(message = "Latitude is required.")
    private Double latitude;

    @NotNull(message = "Longitude is required.")
    private Double longitude;
}
