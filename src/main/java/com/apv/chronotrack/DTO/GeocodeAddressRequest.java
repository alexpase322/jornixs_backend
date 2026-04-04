package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GeocodeAddressRequest {
    @NotBlank(message = "La dirección no puede estar vacía.")
    private String address;
}
