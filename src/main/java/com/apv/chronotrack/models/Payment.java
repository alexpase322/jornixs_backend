package com.apv.chronotrack.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardamos el ID de la factura o sesión de Stripe para referencia
    @Column(name = "stripe_reference_id")
    private String stripeSessionId;

    private Double amount;

    private String currency;

    @Column(name = "customer_email")
    private String customerEmail;

    private String status; // succeeded, failed, pending

    private LocalDateTime date;

    // Se ejecuta automáticamente antes de guardar para poner la fecha actual
    @PrePersist
    protected void onCreate() {
        this.date = LocalDateTime.now();
    }
}