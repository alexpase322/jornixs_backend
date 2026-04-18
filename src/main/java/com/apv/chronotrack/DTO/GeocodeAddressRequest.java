package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GeocodeAddressRequest {
    @NotBlank(message = "Address cannot be empty.")
    private String address;
}
