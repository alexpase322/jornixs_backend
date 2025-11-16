package com.apv.chronotrack.service;

import com.apv.chronotrack.DTO.*;
import com.apv.chronotrack.models.*;
import com.apv.chronotrack.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    // --- DEPENDENCIAS ---
    private final UserRepository userRepository;
    private final TimeLogRepository timeLogRepository;
    private final PayrollCalculationService payrollService;
    private final WorkWeekRepository workWeekRepository;
    private final UserWorkAssignmentRepository assignmentRepository;
    private final WorkLocationRepository workLocationRepository;
    private final WeeklyTimesheetRepository timesheetRepository;

    // --- MÉTODOS DE REPORTES ---

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(User admin) {
        User freshAdmin = findFreshUser(admin);
        Company company = freshAdmin.getCompany();

        // ... (Lógica de totalWorkers y activeWorkersToday)
        long totalWorkers = userRepository.countByCompany(company);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();
        long activeWorkersToday = timeLogRepository.countActiveWorkersBetween(company, startOfDay, endOfDay);

        // Lógica de cálculo de nómina
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        BigDecimal estimatedWeeklyPayroll = BigDecimal.ZERO;
        double totalHoursThisWeek = 0.0;

        Optional<WorkWeek> currentWeekOpt = workWeekRepository.findByCompanyAndStartDateAndEndDate(company, startOfWeek, endOfWeek);

        if (currentWeekOpt.isPresent()) {
            WorkWeek currentWeek = currentWeekOpt.get();
            List<User> workers = userRepository.findByCompanyAndAccountActiveTrue(company).stream()
                    .filter(user -> user.getRole().getRoleName() == RoleName.ROLE_TRABAJADOR)
                    .collect(Collectors.toList());

            List<TimeLog> weeklyLogs = timeLogRepository.findByCompanyAndTimestampBetween(company, startOfWeek.atStartOfDay(), endOfWeek.atTime(LocalTime.MAX));
            Map<Long, List<TimeLog>> logsByWorkerId = weeklyLogs.stream().collect(Collectors.groupingBy(log -> log.getUser().getId()));

            for (User worker : workers) {
                List<TimeLog> workerLogsForWeek = logsByWorkerId.getOrDefault(worker.getId(), List.of());
                // Llama al servicio de cálculo correcto
                WeeklyPaySummaryDto weeklySummary = payrollService.calculateWeeklyPay(worker, currentWeek, workerLogsForWeek);
                totalHoursThisWeek += weeklySummary.getTotalHours();
                estimatedWeeklyPayroll = estimatedWeeklyPayroll.add(weeklySummary.getTotalPay());
            }
        }

        return DashboardStatsDto.builder()
                .totalWorkers(totalWorkers)
                .activeWorkersToday(activeWorkersToday)
                .totalHoursThisWeek(totalHoursThisWeek)
                .estimatedWeeklyPayroll(estimatedWeeklyPayroll.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    @Transactional(readOnly = true)
    public ConsolidatedPayrollReportDto generateConsolidatedPayrollReport(LocalDate startDate, LocalDate endDate, User admin) {
        User freshAdmin = findFreshUser(admin);
        Company company = freshAdmin.getCompany();

        List<WorkWeek> weeksInRange = workWeekRepository.findOverlappingWeeks(company, startDate, endDate);
        List<User> workers = userRepository.findByCompany(company).stream()
                .filter(user -> user.getRole().getRoleName() == RoleName.ROLE_TRABAJADOR)
                .collect(Collectors.toList());

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<TimeLog> allLogsInRange = timeLogRepository.findByCompanyAndTimestampBetween(company, startDateTime, endDateTime);
        Map<Long, List<TimeLog>> logsByWorkerId = allLogsInRange.stream()
                .collect(Collectors.groupingBy(log -> log.getUser().getId()));

        Map<Long, ConsolidatedPayrollEntryDto> consolidatedEntries = workers.stream()
                .collect(Collectors.toMap(User::getId, user -> new ConsolidatedPayrollEntryDto(user.getId(), user.getFullName())));

        for (User worker : workers) {
            List<TimeLog> workerLogs = logsByWorkerId.getOrDefault(worker.getId(), List.of());
            for (WorkWeek week : weeksInRange) {
                List<TimeLog> logsForThisWeek = workerLogs.stream()
                        .filter(log -> log.getWorkWeek().getId().equals(week.getId()))
                        .collect(Collectors.toList());

                if (logsForThisWeek.isEmpty()) continue;

                WeeklyPaySummaryDto weeklySummary = payrollService.calculateWeeklyPay(worker, week, logsForThisWeek);

                ConsolidatedPayrollEntryDto entry = consolidatedEntries.get(worker.getId());
                entry.setTotalRegularHours(entry.getTotalRegularHours() + weeklySummary.getRegularHours());
                entry.setTotalOvertimeHours(entry.getTotalOvertimeHours() + weeklySummary.getOvertimeHours());
                entry.setTotalHours(entry.getTotalHours() + weeklySummary.getTotalHours());
                entry.setTotalPay(entry.getTotalPay().add(weeklySummary.getTotalPay()));
            }
        }

        BigDecimal grandTotalPay = consolidatedEntries.values().stream()
                .map(ConsolidatedPayrollEntryDto::getTotalPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ConsolidatedPayrollReportDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .entries(new java.util.ArrayList<>(consolidatedEntries.values()))
                .grandTotalPay(grandTotalPay)
                .build();
    }

    @Transactional(readOnly = true)
    public DetailedPayrollReportDto generateDetailedPayrollReport(Long workerId, LocalDate startDate, LocalDate endDate, User admin) {
        User worker = findWorkerAndVerifyCompany(workerId, admin);
        Company company = worker.getCompany();

        List<WorkWeek> weeksInRange = workWeekRepository.findOverlappingWeeks(company, startDate, endDate);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<TimeLog> allLogs = timeLogRepository.findByUserAndTimestampBetweenOrderByTimestampAsc(worker, startDateTime, endDateTime);

        Map<Long, List<TimeLog>> logsByWeekId = allLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getWorkWeek().getId()));

        List<WeeklyPaySummaryDto> weeklySummaries = weeksInRange.stream()
                .map(week -> {
                    List<TimeLog> logsForWeek = logsByWeekId.getOrDefault(week.getId(), List.of());
                    return payrollService.calculateWeeklyPay(worker, week, logsForWeek);
                })
                .collect(Collectors.toList());

        Map<LocalDate, List<TimeLog>> logsByDay = allLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getTimestamp().toLocalDate()));
        Map<Long, List<DailySummaryDto>> dailySummariesByWeek = new HashMap<>();
        String currentLocationName = assignmentRepository.findByUserAndIsCurrentTrue(worker)
                .map(assignment -> assignment.getWorkLocation().getName())
                .orElse("N/A");

        for (Map.Entry<LocalDate, List<TimeLog>> entry : logsByDay.entrySet()) {
            LocalDate day = entry.getKey();
            List<TimeLog> dailyLogs = entry.getValue();

            // --- LLAMADA CORREGIDA ---
            // Usa el servicio centralizado para el cálculo
            double dailyHours = payrollService.calculateHoursForLogs(dailyLogs);
            BigDecimal dailyPay = worker.getHourlyRate().multiply(BigDecimal.valueOf(dailyHours));

            DailySummaryDto dailySummary = DailySummaryDto.builder()
                    .date(day)
                    .workLocationName(currentLocationName)
                    .clockInTime(findTimeForEvent(dailyLogs, EventType.INGRESO))
                    .clockInLogId(findLogIdForEvent(dailyLogs, EventType.INGRESO))
                    .startLunchTime(findTimeForEvent(dailyLogs, EventType.INICIO_ALMUERZO))
                    .startLunchLogId(findLogIdForEvent(dailyLogs, EventType.INICIO_ALMUERZO))
                    .endLunchTime(findTimeForEvent(dailyLogs, EventType.FINAL_ALMUERZO)) // <-- CORREGIDO
                    .endLunchLogId(findLogIdForEvent(dailyLogs, EventType.FINAL_ALMUERZO)) // <-- CORREGIDO
                    .clockOutTime(findTimeForEvent(dailyLogs, EventType.SALIDA))
                    .clockOutLogId(findLogIdForEvent(dailyLogs, EventType.SALIDA))
                    .totalHours(dailyHours)
                    .dailyRate(worker.getHourlyRate())
                    .totalPay(dailyPay.setScale(2, RoundingMode.HALF_UP))
                    .build();

            Long weekId = findWeekIdForDate(day, weeksInRange);
            if (weekId != null) {
                dailySummariesByWeek.computeIfAbsent(weekId, k -> new ArrayList<>()).add(dailySummary);
            }
        }

        double grandTotalHours = weeklySummaries.stream().mapToDouble(WeeklyPaySummaryDto::getTotalHours).sum();
        double grandTotalRegularHours = weeklySummaries.stream().mapToDouble(WeeklyPaySummaryDto::getRegularHours).sum();
        double grandTotalOvertimeHours = weeklySummaries.stream().mapToDouble(WeeklyPaySummaryDto::getOvertimeHours).sum();
        BigDecimal grandTotalPay = weeklySummaries.stream().map(WeeklyPaySummaryDto::getTotalPay).reduce(BigDecimal.ZERO, BigDecimal::add);

        return DetailedPayrollReportDto.builder()
                .workerId(workerId)
                .workerName(worker.getFullName())
                .startDate(startDate)
                .endDate(endDate)
                .weeklySummaries(weeklySummaries)
                .dailySummariesByWeek(dailySummariesByWeek)
                .timeLogsByWeek(logsByWeekId.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(log -> new TimeLogDTO(log.getId(), log.getEventType(), log.getTimestamp(), log.getWorkWeek().getId()))
                                        .collect(Collectors.toList())
                        )))
                .grandTotalHours(grandTotalHours)
                .grandTotalRegularHours(grandTotalRegularHours)
                .grandTotalOvertimeHours(grandTotalOvertimeHours)
                .grandTotalPay(grandTotalPay.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    // --- MÉTODOS DE GESTIÓN DE TRABAJADORES ---

    @Transactional(readOnly = true)
    public List<WorkerDto> listWorkers(User admin, String status) {
        User freshAdmin = findFreshUser(admin);
        Company company = freshAdmin.getCompany();
        List<User> workers;
        if ("INACTIVE".equalsIgnoreCase(status)) {
            workers = userRepository.findByCompanyAndAccountActiveFalse(company);
        } else if ("ALL".equalsIgnoreCase(status)) {
            workers = userRepository.findByCompany(company);
        } else {
            workers = userRepository.findByCompanyAndAccountActiveTrue(company);
        }
        return workers.stream()
                .filter(user -> user.getRole().getRoleName() == RoleName.ROLE_TRABAJADOR) // Asumiendo ROLE_TRABAJADOR
                .map(this::convertToWorkerDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkerDto getWorkerById(Long workerId, User admin) {
        User worker = findWorkerAndVerifyCompany(workerId, admin);
        return convertToWorkerDto(worker);
    }

    @Transactional
    public WorkerDto updateWorker(Long workerId, UpdateWorkerRequestDto request, User admin) {
        User worker = findWorkerAndVerifyCompany(workerId, admin);
        worker.setFullName(request.getFullName());
        worker.setHourlyRate(request.getHourlyRate());
        worker.setAccountActive(request.isActive());

        assignmentRepository.findByUserAndIsCurrentTrue(worker).ifPresent(oldAssignment -> {
            oldAssignment.setCurrent(false);
            assignmentRepository.save(oldAssignment);
        });
        if (request.getWorkLocationId() != null) {
            WorkLocation newLocation = workLocationRepository.findById(request.getWorkLocationId())
                    .orElseThrow(() -> new EntityNotFoundException("Lugar de trabajo no encontrado."));
            if (!newLocation.getCompany().getId().equals(admin.getCompany().getId())) {
                throw new SecurityException("Acceso denegado.");
            }
            UserWorkLocationAssignment newAssignment = new UserWorkLocationAssignment();
            newAssignment.setUser(worker);
            newAssignment.setWorkLocation(newLocation);
            newAssignment.setCurrent(true);
            assignmentRepository.save(newAssignment);
        }
        User updatedWorker = userRepository.save(worker);
        return convertToWorkerDto(updatedWorker);
    }

    @Transactional
    public void deactivateWorker(Long workerId, User admin) {
        User worker = findWorkerAndVerifyCompany(workerId, admin);
        worker.setAccountActive(false);
        userRepository.save(worker);
    }

    @Transactional
    public TimeLogDTO performManualTimeCorrection(ManualTimeLogRequestDto request, User admin) {
        User worker = findWorkerAndVerifyCompany(request.getWorkerId(), admin);
        WorkWeek workWeek = findOrCreateWorkWeekForDate(request.getTimestamp().toLocalDate(), worker.getCompany());
        WeeklyTimesheet timesheet = findOrCreateTimesheet(worker, workWeek);
        if (timesheet.getStatus() == TimesheetStatus.APPROVED) {
            throw new IllegalStateException("Acción denegada: No se puede modificar un registro de una semana que ya ha sido aprobada.");
        }
        TimeLog timeLog;
        if (request.getTimeLogIdToEdit() != null) {
            timeLog = timeLogRepository.findById(request.getTimeLogIdToEdit())
                    .orElseThrow(() -> new EntityNotFoundException("Registro de tiempo no encontrado."));
            if (!timeLog.getUser().getId().equals(worker.getId())) {
                throw new SecurityException("Este registro de tiempo no pertenece al trabajador especificado.");
            }
            timeLog.setEventType(request.getEventType());
            timeLog.setTimestamp(request.getTimestamp());
        } else {
            timeLog = new TimeLog();
            timeLog.setUser(worker);
            timeLog.setEventType(request.getEventType());
            timeLog.setTimestamp(request.getTimestamp());
        }
        timeLog.setWorkWeek(workWeek);
        TimeLog savedLog = timeLogRepository.save(timeLog);
        return new TimeLogDTO(savedLog.getId(), savedLog.getEventType(), savedLog.getTimestamp(), savedLog.getWorkWeek().getId());
    }

    @Transactional
    public void deleteTimeLog(Long timeLogId, User admin) {
        TimeLog timeLog = timeLogRepository.findById(timeLogId)
                .orElseThrow(() -> new EntityNotFoundException("Registro de tiempo no encontrado."));
        findWorkerAndVerifyCompany(timeLog.getUser().getId(), admin);
        WeeklyTimesheet timesheet = findOrCreateTimesheet(timeLog.getUser(), timeLog.getWorkWeek());
        if (timesheet.getStatus() == TimesheetStatus.APPROVED) {
            throw new IllegalStateException("Acción denegada: No se puede eliminar un registro de una semana que ya ha sido aprobada.");
        }
        timeLogRepository.delete(timeLog);
    }

    // --- MÉTODOS PRIVADOS AUXILIARES ---

    private User findFreshUser(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario con ID " + user.getId() + " no encontrado."));
    }

    private User findWorkerAndVerifyCompany(Long workerId, User admin) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new EntityNotFoundException("Trabajador no encontrado con ID: " + workerId));
        if (!worker.getCompany().getId().equals(admin.getCompany().getId())) {
            throw new SecurityException("Acceso denegado: No tienes permiso para modificar este trabajador.");
        }
        return worker;
    }

    private WorkerDto convertToWorkerDto(User user) {
        Optional<UserWorkLocationAssignment> currentAssignment = assignmentRepository.findByUserAndIsCurrentTrue(user);
        return WorkerDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .hourlyRate(user.getHourlyRate())
                .isActive(user.isAccountActive())
                .workLocationId(currentAssignment.map(a -> a.getWorkLocation().getId()).orElse(null))
                .workLocationName(currentAssignment.map(a -> a.getWorkLocation().getName()).orElse("Sin Asignar"))
                .build();
    }

    private LocalTime findTimeForEvent(List<TimeLog> logs, EventType eventType) {
        return logs.stream()
                .filter(log -> log.getEventType() == eventType)
                .map(log -> log.getTimestamp().toLocalTime())
                .findFirst()
                .orElse(null);
    }

    private Long findLogIdForEvent(List<TimeLog> logs, EventType eventType) {
        return logs.stream()
                .filter(log -> log.getEventType() == eventType)
                .map(TimeLog::getId)
                .findFirst()
                .orElse(null);
    }

    private Long findWeekIdForDate(LocalDate date, List<WorkWeek> weeks) {
        for (WorkWeek week : weeks) {
            if (!date.isBefore(week.getStartDate()) && !date.isAfter(week.getEndDate())) {
                return week.getId();
            }
        }
        return null;
    }

    private WorkWeek findOrCreateWorkWeekForDate(LocalDate date, Company company) {
        LocalDate startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return workWeekRepository.findByCompanyAndStartDateAndEndDate(company, startOfWeek, endOfWeek)
                .orElseGet(() -> {
                    WorkWeek newWeek = new WorkWeek();
                    newWeek.setCompany(company);
                    newWeek.setStartDate(startOfWeek);
                    newWeek.setEndDate(endOfWeek);
                    return workWeekRepository.save(newWeek);
                });
    }

    private WeeklyTimesheet findOrCreateTimesheet(User user, WorkWeek workWeek) {
        return timesheetRepository.findByUserAndWorkWeek(user, workWeek)
                .orElseGet(() -> {
                    WeeklyTimesheet newTimesheet = new WeeklyTimesheet();
                    newTimesheet.setUser(user);
                    newTimesheet.setWorkWeek(workWeek);
                    return timesheetRepository.save(newTimesheet);
                });
    }
}