package com.apv.chronotrack.repository;

import com.apv.chronotrack.models.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // Aquí podríamos añadir métodos de búsqueda si necesitamos consultar el log
}
