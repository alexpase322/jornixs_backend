package com.apv.chronotrack.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkLocationDto {
    private Long id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double geofenceRadiusMeters;
}