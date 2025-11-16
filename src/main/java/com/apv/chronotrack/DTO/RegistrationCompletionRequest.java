package com.apv.chronotrack.DTO;

import lombok.Data;

@Data
public class RegistrationCompletionRequest {

    // --- Datos de Acceso y Perfil ---
    private String token;  // Usado para identificar al usuario a actualizar
    private String fullName; // Corresponde a la Línea 1 del W-9: "Name"
    private String password;
    private String address;

    // --- Información del Formulario W-9 ---

    // Línea 2: "Business name/disregarded entity name, if different from above"
    private String businessName;

    // Línea 3: Federal tax classification (Individual, C Corp, S Corp, etc.)
    // Se puede manejar como un String simple o un Enum en el backend.
    private String taxClassification;

    // Línea 4: Exemptions (optional)
    private String exemptPayeeCode;
    private String fatcaExemptionCode;

    // Línea 5 & 6: Address
    private String streetAddress; // Street (number, street, and apt. or suite no.)
    private String cityStateZip;  // City, state, and ZIP code

    // Parte I: Taxpayer Identification Number (TIN)
    // El frontend debería asegurar que se envíe SSN o EIN, pero no ambos.
    private String ssn; // Social Security Number
    private String ein; // Employer Identification Number

    // Parte II: Certification
    // El acto de enviar el formulario con una casilla marcada en el frontend sirve como firma.
    private boolean certifiedUnderPenaltiesOfPerjury;
}
