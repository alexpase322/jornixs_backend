package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyRegistrationRequest {
    @NotBlank
    private String token;
    // --- Datos de la Compañía ---
    @NotBlank(message = "El nombre de la compañía no puede estar vacío.")
    private String companyName;

    @NotBlank(message = "La dirección de la compañía no puede estar vacía.")
    private String companyAddress;


    private String companyPhoneNumber;

    @NotBlank(message = "El EIN de la compañía no puede estar vacío.")
    private String ein; // Employer Identification Number (TIN de la empresa)

    // --- Datos del Administrador ---
    @NotBlank(message = "El nombre del administrador no puede estar vacío.")
    private String adminFullName;

    @NotBlank(message = "El email del administrador no puede estar vacío.")
    @Email(message = "El formato del email no es válido.")
    private String adminEmail;

    @NotBlank(message = "La contraseña no puede estar vacía.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
    private String adminPassword;

    private Double workLatitude;
    private Double workLongitude;
    private Double geofenceRadiusMeters;

    @AssertTrue(message = "Debes aceptar los términos y condiciones.")
    private boolean termsAccepted;
}
