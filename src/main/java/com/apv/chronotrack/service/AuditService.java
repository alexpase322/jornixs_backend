package com.apv.chronotrack.service;

import com.apv.chronotrack.models.AuditLog;
import com.apv.chronotrack.models.Company;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async // Hacemos que el registro sea asíncrono para no ralentizar la acción principal
    public void logAction(User user, Company company, String action, String targetEntity, Long targetEntityId, String details) {
        AuditLog logEntry = new AuditLog(user, company, action, targetEntity, targetEntityId, details);
        auditLogRepository.save(logEntry);
    }

    // Sobrecarga útil si la acción no la realiza un usuario específico (ej: tareas programadas)
    @Async
    public void logSystemAction(Company company, String action, String targetEntity, Long targetEntityId, String details) {
        AuditLog logEntry = new AuditLog(null, company, action, targetEntity, targetEntityId, details);
        auditLogRepository.save(logEntry);
    }
}
