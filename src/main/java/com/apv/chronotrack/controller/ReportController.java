package com.apv.chronotrack.controller;

import com.apv.chronotrack.DTO.DetailedPayrollReportDto;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AdminService adminService; // Reutilizamos el servicio

    @GetMapping("/detailed/me")
    @PreAuthorize("hasAuthority('ROLE_TRABAJADOR')")
    public ResponseEntity<DetailedPayrollReportDto> getMyDetailedReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal User worker) {

        return ResponseEntity.ok(adminService.generateDetailedPayrollReport(worker.getId(), startDate, endDate, worker));
    }
}