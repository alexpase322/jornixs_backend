package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrUpdateLocationRequest {
    @NotBlank(message = "Name cannot be empty.")
    private String name;

    private String address;

    // Optional: if not provided, will be geocoded from address.
    private Double latitude;

    // Optional: if not provided, will be geocoded from address.
    private Double longitude;

    @NotNull(message = "Geofence radius is required.")
    private Double geofenceRadiusMeters;
}
