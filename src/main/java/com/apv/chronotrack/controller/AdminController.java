package com.apv.chronotrack.controller;

import com.apv.chronotrack.DTO.*;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.service.AdminService;
import com.apv.chronotrack.service.FileExportService;
import com.lowagie.text.DocumentException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final FileExportService fileExportService; // <-- Inyectar el nuevo servicio

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats(@AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(adminService.getDashboardStats(admin));
    }
    // --- Nuevos Endpoints de Gestión de Trabajadores ---

    @GetMapping("/workers")
    public ResponseEntity<List<WorkerDto>> listWorkers(
            @AuthenticationPrincipal User admin,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status) { // <-- Cambio aquí

        return ResponseEntity.ok(adminService.listWorkers(admin, status));
    }

    @PutMapping("/workers/{id}")
    public ResponseEntity<WorkerDto> updateWorker(
            @PathVariable Long id,
            @RequestBody UpdateWorkerRequestDto request,
            @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(adminService.updateWorker(id, request, admin));
    }

    @DeleteMapping("/workers/{id}")
    public ResponseEntity<Void> deactivateWorker(@PathVariable Long id, @AuthenticationPrincipal User admin) {
        adminService.deactivateWorker(id, admin);
        return ResponseEntity.noContent().build(); // Devuelve un 204 No Content, estándar para DELETE
    }

    @GetMapping("/reports/consolidated-payroll")
    public ResponseEntity<ConsolidatedPayrollReportDto> getConsolidatedPayrollReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User admin) {

        return ResponseEntity.ok(adminService.generateConsolidatedPayrollReport(startDate, endDate, admin));
    }

    @GetMapping("/workers/{id}")
    public ResponseEntity<WorkerDto> getWorkerById(@PathVariable Long id, @AuthenticationPrincipal User admin) {
        return ResponseEntity.ok(adminService.getWorkerById(id, admin));
    }

    @GetMapping("/reports/detailed-payroll/{workerId}")
    public ResponseEntity<DetailedPayrollReportDto> getDetailedPayrollReport(
            @PathVariable Long workerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User admin) {

        return ResponseEntity.ok(adminService.generateDetailedPayrollReport(workerId, startDate, endDate, admin));
    }

    @GetMapping("/reports/consolidated-payroll/pdf")
    public ResponseEntity<InputStreamResource> exportConsolidatedReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User admin) throws DocumentException {

        ConsolidatedPayrollReportDto report = adminService.generateConsolidatedPayrollReport(startDate, endDate, admin);
        ByteArrayInputStream pdf = fileExportService.generateConsolidatedPdf(report);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=reporte-nomina.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }

    @GetMapping("/reports/consolidated-payroll/excel")
    public ResponseEntity<InputStreamResource> exportConsolidatedReportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User admin) throws IOException {

        ConsolidatedPayrollReportDto report = adminService.generateConsolidatedPayrollReport(startDate, endDate, admin);
        ByteArrayInputStream excel = fileExportService.generateConsolidatedExcel(report);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=reporte-nomina.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(excel));
    }

    @GetMapping("/reports/detailed-payroll/{workerId}/pdf")
    public ResponseEntity<InputStreamResource> exportDetailedReportPdf(
            @PathVariable Long workerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User admin) throws DocumentException, IOException {

        DetailedPayrollReportDto report = adminService.generateDetailedPayrollReport(workerId, startDate, endDate, admin);
        ByteArrayInputStream pdf = fileExportService.generateDetailedPdf(report);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=reporte-detallado-" + report.getWorkerName() + ".pdf");

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(new InputStreamResource(pdf));
    }

    @GetMapping("/reports/detailed-payroll/{workerId}/excel")
    public ResponseEntity<InputStreamResource> exportDetailedReportExcel(
            @PathVariable Long workerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User admin) throws IOException {

        DetailedPayrollReportDto report = adminService.generateDetailedPayrollReport(workerId, startDate, endDate, admin);
        ByteArrayInputStream excel = fileExportService.generateDetailedExcel(report);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=reporte-detallado-" + report.getWorkerName() + ".xlsx");

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_OCTET_STREAM).body(new InputStreamResource(excel));
    }

    @PostMapping("/timelogs/correction")
    public ResponseEntity<TimeLogDTO> correctTimeLog(
            @Valid @RequestBody ManualTimeLogRequestDto request,
            @AuthenticationPrincipal User admin) {

        return ResponseEntity.ok(adminService.performManualTimeCorrection(request, admin));
    }

    @DeleteMapping("/timelogs/{timeLogId}")
    public ResponseEntity<Void> deleteTimeLog(@PathVariable Long timeLogId, @AuthenticationPrincipal User admin) {
        adminService.deleteTimeLog(timeLogId, admin);
        return ResponseEntity.noContent().build();
    }
}
