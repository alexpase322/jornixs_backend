package com.apv.chronotrack.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // El usuario que realizó la acción (puede ser null si es una acción del sistema)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false) // La compañía a la que afecta la acción
    private Company company;

    @Column(nullable = false)
    private String action; // Ej: "USER_LOGIN", "TIMESHEET_APPROVED", "WORKER_UPDATED"

    @Column(name = "target_entity")
    private String targetEntity; // Ej: "User", "WeeklyTimesheet"

    @Column(name = "target_entity_id")
    private Long targetEntityId; // El ID del objeto afectado

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String details; // Detalles adicionales (ej: "Cambió el estado a APPROVED")

    @PrePersist
    public void onPrePersist() {
        timestamp = LocalDateTime.now();
    }

    // Constructor útil
    public AuditLog(User user, Company company, String action, String targetEntity, Long targetEntityId, String details) {
        this.user = user;
        this.company = company;
        this.action = action;
        this.targetEntity = targetEntity;
        this.targetEntityId = targetEntityId;
        this.details = details;
    }
}