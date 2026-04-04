package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrUpdateLocationRequest {
    @NotBlank(message = "El nombre no puede estar vacío.")
    private String name;

    private String address;

    // Opcional: si no se envía, se intentará obtener desde la dirección por geocodificación.
    private Double latitude;

    // Opcional: si no se envía, se intentará obtener desde la dirección por geocodificación.
    private Double longitude;

    @NotNull(message = "El radio del geofence es requerido.")
    private Double geofenceRadiusMeters;
}
