package com.apv.chronotrack.models;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "address")
    private String address;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "ein", unique = true)
    private String ein;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Relación: Una Compañía tiene muchos Usuarios
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("company-users") // <-- AÑADIR
    private List<User> users;

    @Column(name = "work_latitude")
    private Double workLatitude;

    @Column(name = "work_longitude")
    private Double workLongitude;

    // Radio en metros para la cerca virtual
    @Column(name = "geofence_radius_meters")
    private Double geofenceRadiusMeters;

    private String subscriptionPlan;

    private String stripeCustomerId; // ID del cliente en Stripe
    private String stripeSubscriptionId; // ID de la suscripción activa

    @Column(name = "plan_price_id")
    private String planPriceId;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus subscriptionStatus;
}
