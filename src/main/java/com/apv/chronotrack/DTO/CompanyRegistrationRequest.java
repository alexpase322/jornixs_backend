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

    // --- Company Data ---
    @NotBlank(message = "Company name cannot be empty.")
    private String companyName;

    @NotBlank(message = "Company address cannot be empty.")
    private String companyAddress;

    private String companyPhoneNumber;

    @NotBlank(message = "Company EIN cannot be empty.")
    private String ein; // Employer Identification Number

    // --- Administrator Data ---
    @NotBlank(message = "Administrator name cannot be empty.")
    private String adminFullName;

    @NotBlank(message = "Administrator email cannot be empty.")
    @Email(message = "Invalid email format.")
    private String adminEmail;

    @NotBlank(message = "Password cannot be empty.")
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    private String adminPassword;

    private Double workLatitude;
    private Double workLongitude;
    private Double geofenceRadiusMeters;

    @AssertTrue(message = "You must accept the terms and conditions.")
    private boolean termsAccepted;

    private String logoUrl;
}
