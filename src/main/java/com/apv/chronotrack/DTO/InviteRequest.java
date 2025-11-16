package com.apv.chronotrack.DTO;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteRequest {
    @NotBlank
    @Email
    private String email;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal hourlyRate;

    // --- CAMPO AÑADIDO ---
    // Será el ID del WorkLocation al que se asignará el trabajador. Es opcional.
    private Long workLocationId;
}
