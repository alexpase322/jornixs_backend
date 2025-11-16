package com.apv.chronotrack.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "work_locations")
public class WorkLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Ej: "Oficina Central", "Bodega Sur"

    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // El radio ahora pertenece a cada lugar de trabajo, no a la compañía
    @Column(name = "geofence_radius_meters", nullable = false)
    private Double geofenceRadiusMeters;

    // Relación: Muchos lugares de trabajo pertenecen a una compañía
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}
