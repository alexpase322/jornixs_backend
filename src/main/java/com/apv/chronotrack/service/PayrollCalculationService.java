package com.apv.chronotrack.service;

import com.apv.chronotrack.DTO.WeeklyPaySummaryDto;
import com.apv.chronotrack.DTO.WorkWeekDto;
import com.apv.chronotrack.models.EventType;
import com.apv.chronotrack.models.TimeLog;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.models.WorkWeek;
import com.apv.chronotrack.repository.WeeklyTimesheetRepository; // Necesitarás este
import com.apv.chronotrack.models.WeeklyTimesheet; // Y este
import com.apv.chronotrack.models.TimesheetStatus; // Y este
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator; // <-- Importante
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollCalculationService {

    // Asegúrate de tener este repositorio inyectado
    private final WeeklyTimesheetRepository timesheetRepository;
    private static final double WEEKLY_HOURS_THRESHOLD = 40.0;
    private static final BigDecimal OVERTIME_RATE_MULTIPLIER = new BigDecimal("1.5");

    /**
     * MÉTODO 1: CALCULAR PAGO SEMANAL
     * Este método calcula el pago de una semana, incluyendo horas extras.
     * Ahora recibe los logs como parámetro para ser más eficiente.
     */
    public WeeklyPaySummaryDto calculateWeeklyPay(User user, WorkWeek week, List<TimeLog> logs) {

        // 1. Llama al método robusto para calcular el total de horas
        double totalHours = calculateHoursForLogs(logs);

        double regularHours = 0;
        double overtimeHours = 0;

        if (totalHours > WEEKLY_HOURS_THRESHOLD) {
            regularHours = WEEKLY_HOURS_THRESHOLD;
            overtimeHours = totalHours - WEEKLY_HOURS_THRESHOLD;
        } else {
            regularHours = totalHours;
        }

        BigDecimal hourlyRate = user.getHourlyRate();
        BigDecimal overtimeRate = hourlyRate.multiply(OVERTIME_RATE_MULTIPLIER);

        BigDecimal regularPay = hourlyRate.multiply(BigDecimal.valueOf(regularHours));
        BigDecimal overtimePay = overtimeRate.multiply(BigDecimal.valueOf(overtimeHours));
        BigDecimal totalPay = regularPay.add(overtimePay).setScale(2, RoundingMode.HALF_UP);

        // 2. Buscamos el estado de la hoja de horas
        WeeklyTimesheet timesheet = timesheetRepository.findByUserAndWorkWeek(user, week).orElse(null);
        TimesheetStatus status = (timesheet != null) ? timesheet.getStatus() : TimesheetStatus.OPEN;

        // 3. Construimos el DTO de la semana
        WorkWeekDto weekDto = WorkWeekDto.builder()
                .id(week.getId())
                .startDate(week.getStartDate())
                .endDate(week.getEndDate())
                .build();

        return WeeklyPaySummaryDto.builder()
                .workWeek(weekDto)
                .workerId(user.getId())
                .workerName(user.getFullName())
                .totalHours(totalHours)
                .regularHours(regularHours)
                .overtimeHours(overtimeHours)
                .regularPay(regularPay)
                .overtimePay(overtimePay)
                .totalPay(totalPay)
                .status(status) // <-- Asegúrate de que el DTO incluya el estado
                .build();
    }

    /**
     * MÉTODO 2: CALCULAR HORAS
     * Este es el método robusto y centralizado para calcular el total de horas.
     */
    public double calculateHoursForLogs(List<TimeLog> logs) {
        double totalMinutes = 0;
        LocalDateTime startTime = null;

        // 1. Creamos una copia de la lista para poder ordenarla de forma segura.
        List<TimeLog> sortedLogs = new ArrayList<>(logs);
        sortedLogs.sort(Comparator.comparing(TimeLog::getTimestamp));

        for (TimeLog log : sortedLogs) {
            EventType event = log.getEventType();

            // Lógica de "Inicio de Conteo" (Solo si no estamos ya contando)
            if ( (event.name().equals("INGRESO") || event.name().equals("FINAL_ALMUERZO"))
                    && startTime == null ) { // <-- ESTA ES LA CORRECCIÓN CLAVE

                startTime = log.getTimestamp();
            }
            // Lógica de "Fin de Conteo" (Solo si SÍ estamos contando)
            else if ( (event.name().equals("SALIDA") || event.name().equals("INICIO_ALMUERZO"))
                    && startTime != null ) {

                totalMinutes += Duration.between(startTime, log.getTimestamp()).toMinutes();
                startTime = null; // Reseteamos el contador para el siguiente intervalo
            }
            // Si es un INGRESO y ya estábamos contando, o una SALIDA y no estábamos contando,
            // esta lógica los ignora, previniendo errores de cálculo.
        }

        return Math.round((totalMinutes / 60.0) * 100.0) / 100.0;
    }
}