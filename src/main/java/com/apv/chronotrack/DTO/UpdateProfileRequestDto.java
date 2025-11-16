package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateProfileRequestDto {

    // Datos del Perfil Básico
    private String fullName;

    // Nueva contraseña (opcional)
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres.")
    private String newPassword;

    // Datos del W-9 y Dirección
    private String streetAddress;
    private String cityStateZip;

    @Pattern(regexp = "^(?!000|666)[0-8][0-9]{2}-(?!00)[0-9]{2}-(?!0000)[0-9]{4}$", message = "El formato del SSN debe ser ###-##-####.")
    private String ssn;
}