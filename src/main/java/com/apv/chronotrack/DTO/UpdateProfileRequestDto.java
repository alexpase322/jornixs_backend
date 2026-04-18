package com.apv.chronotrack.DTO;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateProfileRequestDto {

    // Basic Profile Data
    private String fullName;
    private String email;

    // New password (optional)
    @Size(min = 8, message = "New password must be at least 8 characters long.")
    private String newPassword;

    // W-9 and Address Data
    private String streetAddress;
    private String cityStateZip;

    @Pattern(regexp = "^(?!000|666)[0-8][0-9]{2}-(?!00)[0-9]{2}-(?!0000)[0-9]{4}$", message = "SSN format must be ###-##-####.")
    private String ssn;
}
