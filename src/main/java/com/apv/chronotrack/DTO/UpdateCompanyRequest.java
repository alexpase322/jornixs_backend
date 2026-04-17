package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCompanyRequest {
    @NotBlank(message = "El nombre de la empresa no puede estar vacio.")
    private String companyName;

    private String address;
    private String phoneNumber;
    private Double workLatitude;
    private Double workLongitude;
    private Double geofenceRadiusMeters;
}
