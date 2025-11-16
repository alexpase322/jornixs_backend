package com.apv.chronotrack.DTO;

// Usamos un record para una estructura de datos simple e inmutable.
public record W9Data(
        String businessName,
        String taxClassification,
        String exemptPayeeCode,
        String fatcaExemptionCode,
        String streetAddress,
        String cityStateZip,
        String ssn,
        String ein
) {}
