package com.apv.chronotrack.controller;

import com.apv.chronotrack.DTO.TimesheetSummaryDto;
import com.apv.chronotrack.models.TimesheetStatus;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.service.TimesheetService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetService timesheetService;

    // --- ENDPOINT PARA EL TRABAJADOR ---

    /**
     * Un trabajador envía su hoja de horas de una semana específica.
     */
    @PostMapping("/api/worker/timesheets/submit/{workWeekId}")
    public ResponseEntity<String> submitTimesheet(
            @AuthenticationPrincipal User user,
            @PathVariable Long workWeekId) {
        timesheetService.submitTimesheet(user, workWeekId);
        return ResponseEntity.ok("Hoja de horas enviada correctamente.");
    }

    // --- ENDPOINTS PARA EL ADMINISTRADOR ---

    /**
     * Un administrador aprueba una hoja de horas.
     */
    @PostMapping("/api/admin/timesheets/{timesheetId}/approve")
    public ResponseEntity<String> approveTimesheet(
            @AuthenticationPrincipal User admin,
            @PathVariable Long timesheetId) {
        timesheetService.approveTimesheet(timesheetId, admin);
        return ResponseEntity.ok("Hoja de horas aprobada.");
    }

    /**
     * Un administrador rechaza una hoja de horas, proporcionando un motivo.
     */
    @PostMapping("/api/admin/timesheets/{timesheetId}/reject")
    public ResponseEntity<String> rejectTimesheet(
            @AuthenticationPrincipal User admin,
            @PathVariable Long timesheetId,
            @RequestBody RejectionRequest payload) {
        timesheetService.rejectTimesheet(timesheetId, payload.getReason(), admin);
        return ResponseEntity.ok("Hoja de horas rechazada.");
    }

    // DTO simple para recibir el motivo del rechazo
    @Data
    private static class RejectionRequest {
        private String reason;
    }

    @GetMapping("/api/admin/timesheets/submitted")
    public ResponseEntity<List<TimesheetSummaryDto>> getSubmittedTimesheets(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(timesheetService.getSubmittedTimesheets(admin));
    }

    @GetMapping("/api/timesheets")
    public ResponseEntity<List<TimesheetSummaryDto>> getFilteredTimesheets(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) TimesheetStatus status,
            @RequestParam(required = false) Long workerId) {
        return ResponseEntity.ok(timesheetService.getFilteredTimesheets(user, status, workerId));
    }

    // Endpoint para que un trabajador reabra una hoja de horas rechazada
    @PostMapping("/api/worker/timesheets/{timesheetId}/resubmit")
    public ResponseEntity<String> resubmitTimesheet(
            @AuthenticationPrincipal User user,
            @PathVariable Long timesheetId) {
        timesheetService.resubmitTimesheet(user, timesheetId);
        return ResponseEntity.ok("La hoja de horas ha sido reabierta para su edición.");
    }
}