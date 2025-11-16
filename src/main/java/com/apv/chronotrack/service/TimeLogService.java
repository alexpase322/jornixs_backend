package com.apv.chronotrack.service;

import com.apv.chronotrack.DTO.ClockInRequestDto;
import com.apv.chronotrack.DTO.TimeLogDTO;
import com.apv.chronotrack.DTO.WorkerCorrectionRequestDto;
import com.apv.chronotrack.models.*;
import com.apv.chronotrack.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeLogService {

    private final TimeLogRepository timeLogRepository;
    private final UserRepository userRepository;
    private final UserWorkAssignmentRepository assignmentRepository;
    private final WorkWeekRepository workWeekRepository;
    private final WeeklyTimesheetRepository timesheetRepository;

    // --- Acciones del Trabajador ---
    @Transactional
    public TimeLogDTO recordClockIn(User user, ClockInRequestDto request) {

        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        boolean alreadyClockedInToday = timeLogRepository.existsByUserAndEventTypeAndTimestampBetween(freshUser, EventType.INGRESO, startOfDay, endOfDay);

        if (alreadyClockedInToday) {
            throw new IllegalStateException("Ya has marcado tu ingreso hoy. No puedes marcar el ingreso más de una vez por día.");
        }
        // --- LÓGICA DE VALIDACIÓN DE HOJA DE HORAS ---
        WorkWeek workWeek = findOrCreateWorkWeekForDate(LocalDate.now(), freshUser.getCompany());
        WeeklyTimesheet timesheet = findOrCreateTimesheet(freshUser, workWeek);

        if (timesheet.getStatus() != TimesheetStatus.OPEN) {
            throw new IllegalStateException("No se puede registrar tiempo. La hoja de horas de esta semana ya ha sido enviada o procesada.");
        }
        // 1. Validar que la acción sea permitida (no hacer clock-in dos veces)
        validateAction(freshUser, EventType.INGRESO);

        // 2. ¡NUEVA VALIDACIÓN! Verificar si el usuario está dentro de la zona de trabajo.
        if (!isWithinGeofence(freshUser, request.getLatitude(), request.getLongitude())) {
            throw new IllegalStateException("No se puede marcar el ingreso. No te encuentras en la zona de trabajo permitida.");
        }

        // 3. Si todo es correcto, registrar el evento.

        TimeLog newLog = new TimeLog();
        newLog.setUser(freshUser);
        newLog.setEventType(EventType.INGRESO);
        newLog.setTimestamp(LocalDateTime.now());
        newLog.setWorkWeek(workWeek);

        TimeLog savedLog = timeLogRepository.save(newLog);
        return new TimeLogDTO(savedLog.getId(), savedLog.getEventType(), savedLog.getTimestamp(), savedLog.getWorkWeek().getId());
    }

    // En la clase TimeLogService.java
// ...

    private boolean isWithinGeofence(User user, double userLat, double userLon) {
        // Buscamos la asignación ACTIVA del usuario en la nueva tabla
        Optional<UserWorkLocationAssignment> currentAssignmentOpt = assignmentRepository.findByUserAndIsCurrentTrue(user);

        if (currentAssignmentOpt.isEmpty()) {
            return false; // Si no tiene una asignación activa, no puede marcar ingreso.
        }

        WorkLocation workLocation = currentAssignmentOpt.get().getWorkLocation();

        Double locationLat = workLocation.getLatitude();
        Double locationLon = workLocation.getLongitude();
        Double radius = workLocation.getGeofenceRadiusMeters();

        if (locationLat == null || locationLon == null || radius == null) {
            return true;
        }

        double distance = calculateDistance(locationLat, locationLon, userLat, userLon);
        return distance <= radius;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en kilómetros
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // Convertir a metros
        return distance;
    }

    @Transactional
    public TimeLogDTO recordTimeLog(User user, EventType eventType) {
        // Lógica de validación para asegurar un flujo correcto (ej. no marcar salida sin haber entrado)
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        validateAction(user, eventType);
        WorkWeek workWeek = findOrCreateWorkWeekForDate(LocalDate.now(), freshUser.getCompany());
        WeeklyTimesheet timesheet = findOrCreateTimesheet(freshUser, workWeek);

        if (timesheet.getStatus() != TimesheetStatus.OPEN) {
            throw new IllegalStateException("No se puede registrar tiempo. La hoja de horas de esta semana ya ha sido enviada o procesada.");
        }


        TimeLog newLog = new TimeLog();
        newLog.setUser(freshUser);
        newLog.setEventType(eventType);
        newLog.setTimestamp(LocalDateTime.now());
        newLog.setWorkWeek(findOrCreateWorkWeekForDate(newLog.getTimestamp().toLocalDate(), freshUser.getCompany()));
        TimeLog savedLog = timeLogRepository.save(newLog);
        return new TimeLogDTO(savedLog.getId(), savedLog.getEventType(), savedLog.getTimestamp(), savedLog.getWorkWeek().getId());
    }

    // --- Consultas del Trabajador ---

    public List<TimeLogDTO> getTodaysLogs(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return findLogsAndConvertToDto(user, startOfDay, endOfDay);
    }

    public List<TimeLogDTO> getCurrentWeekLogs(User user) {
        LocalDateTime startOfWeek = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime endOfWeek = LocalDate.now().with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
        return findLogsAndConvertToDto(user, startOfWeek, endOfWeek);
    }

    // --- Métodos Privados Auxiliares ---

    private void validateAction(User user, EventType newAction) {
        // Busca el último evento registrado por el usuario.
        EventType lastEventType = timeLogRepository.findTopByUserOrderByTimestampDesc(user)
                .map(TimeLog::getEventType)
                .orElse(null);

        boolean isInvalid = switch (newAction) {
            case INGRESO -> lastEventType != null && lastEventType != EventType.SALIDA;
            case INICIO_ALMUERZO, SALIDA -> lastEventType != EventType.INGRESO && lastEventType != EventType.FINAL_ALMUERZO;
            case FINAL_ALMUERZO -> lastEventType != EventType.INICIO_ALMUERZO;
        };

        if (isInvalid) {
            throw new IllegalStateException("Acción no permitida. El último evento fue: " + lastEventType);
        }
    }

    private List<TimeLogDTO> findLogsAndConvertToDto(User user, LocalDateTime start, LocalDateTime end) {
        return timeLogRepository.findByUserAndTimestampBetweenOrderByTimestampAsc(user, start, end)
                .stream()
                .map(log -> new TimeLogDTO(log.getId(), log.getEventType(), log.getTimestamp(), log.getWorkWeek().getId()))
                .collect(Collectors.toList());
    }

    public List<TimeLogDTO> getLogsByDateRange(User user, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        // Reutilizamos el método auxiliar que ya habíamos creado
        return findLogsAndConvertToDto(user, startDateTime, endDateTime);
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

    @Transactional
    public TimeLogDTO correctWorkerTimeLog(WorkerCorrectionRequestDto request, User worker) {
        // 1. Validar que la hoja de horas esté abierta para edición por el trabajador
        WorkWeek workWeek = findOrCreateWorkWeekForDate(request.getTimestamp().toLocalDate(), worker.getCompany());
        WeeklyTimesheet timesheet = findOrCreateTimesheet(worker, workWeek);

        if (timesheet.getStatus() != TimesheetStatus.OPEN && timesheet.getStatus() != TimesheetStatus.REJECTED) {
            throw new IllegalStateException("Acción denegada: No puedes modificar una semana que ya ha sido enviada o está en proceso de revisión.");
        }

        TimeLog timeLog;

        if (request.getTimeLogIdToEdit() != null) {
            // --- Flujo de Edición ---
            timeLog = timeLogRepository.findById(request.getTimeLogIdToEdit())
                    .orElseThrow(() -> new EntityNotFoundException("Registro de tiempo no encontrado."));

            // Verificación de seguridad: el registro debe pertenecer al trabajador logueado
            if (!timeLog.getUser().getId().equals(worker.getId())) {
                throw new SecurityException("No tienes permiso para editar este registro.");
            }
            timeLog.setEventType(request.getEventType());
            timeLog.setTimestamp(request.getTimestamp());

        } else {
            // --- Flujo de Creación ---
            timeLog = new TimeLog();
            timeLog.setUser(worker);
            timeLog.setEventType(request.getEventType());
            timeLog.setTimestamp(request.getTimestamp());
        }

        // 2. Asignar la semana de trabajo correcta
        timeLog.setWorkWeek(workWeek);
        TimeLog savedLog = timeLogRepository.save(timeLog);

        // 3. Devolver el DTO del registro guardado
        return new TimeLogDTO(savedLog.getId(), savedLog.getEventType(), savedLog.getTimestamp(), savedLog.getWorkWeek().getId());
    }
}