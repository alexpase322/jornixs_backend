package com.apv.chronotrack.controller;

import com.apv.chronotrack.DTO.ClockInRequestDto;
import com.apv.chronotrack.DTO.TimeLogDTO;
import com.apv.chronotrack.DTO.WorkerCorrectionRequestDto;
import com.apv.chronotrack.models.EventType;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.service.TimeLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/worker/timelogs")
@RequiredArgsConstructor
public class TimeLogController {

    private final TimeLogService timeLogService;

    // --- Endpoints de Acción (POST) ---

    @PostMapping("/clock-in")
    public ResponseEntity<TimeLogDTO> clockIn(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ClockInRequestDto request) { // <-- Cambio aquí

        return ResponseEntity.ok(timeLogService.recordClockIn(user, request));
    }

    @PostMapping("/start-lunch")
    public ResponseEntity<TimeLogDTO> startLunch(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(timeLogService.recordTimeLog(user, EventType.INICIO_ALMUERZO));
    }

    @PostMapping("/end-lunch")
    public ResponseEntity<TimeLogDTO> endLunch(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(timeLogService.recordTimeLog(user, EventType.FINAL_ALMUERZO));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<TimeLogDTO> clockOut(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(timeLogService.recordTimeLog(user, EventType.SALIDA));
    }

    // --- Endpoints de Consulta (GET) ---

    @GetMapping("/today")
    public ResponseEntity<List<TimeLogDTO>> getTodaysLogs(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(timeLogService.getTodaysLogs(user));
    }

    @GetMapping("/week")
    public ResponseEntity<List<TimeLogDTO>> getWeeklyLogs(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(timeLogService.getCurrentWeekLogs(user));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<TimeLogDTO>> getLogsByDateRange(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(timeLogService.getLogsByDateRange(user, startDate, endDate));
    }

    @PostMapping("/correction")
    public ResponseEntity<TimeLogDTO> correctWorkerTimeLog(
            @Valid @RequestBody WorkerCorrectionRequestDto request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(timeLogService.correctWorkerTimeLog(request, user));
    }
}