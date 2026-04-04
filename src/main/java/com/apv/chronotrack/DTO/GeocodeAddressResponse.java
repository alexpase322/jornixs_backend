package com.apv.chronotrack.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GeocodeAddressResponse {
    private Double latitude;
    private Double longitude;
    private String normalizedAddress;
}
