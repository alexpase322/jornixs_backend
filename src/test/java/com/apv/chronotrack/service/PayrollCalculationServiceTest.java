package com.apv.chronotrack.service;

import com.apv.chronotrack.DTO.WeeklyPaySummaryDto;
import com.apv.chronotrack.models.*;
import com.apv.chronotrack.repository.WeeklyTimesheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollCalculationServiceTest {

    @Mock
    private WeeklyTimesheetRepository timesheetRepository;

    @InjectMocks
    private PayrollCalculationService payrollService;

    private User testWorker;
    private WorkWeek testWeek;

    @BeforeEach
    void setUp() {
        testWorker = new User();
        testWorker.setId(1L);
        testWorker.setFullName("Test Worker");
        testWorker.setHourlyRate(new BigDecimal("15.00"));

        testWeek = new WorkWeek();
        testWeek.setId(1L);
        testWeek.setStartDate(LocalDate.of(2026, 4, 6));
        testWeek.setEndDate(LocalDate.of(2026, 4, 12));
    }

    @Test
    @DisplayName("calculateHoursForLogs - Jornada simple de 8 horas")
    void calculateHours_simpleWorkDay() {
        List<TimeLog> logs = List.of(
                createLog(EventType.INGRESO, LocalDateTime.of(2026, 4, 6, 8, 0)),
                createLog(EventType.SALIDA, LocalDateTime.of(2026, 4, 6, 16, 0))
        );

        double hours = payrollService.calculateHoursForLogs(logs);
        assertEquals(8.0, hours);
    }

    @Test
    @DisplayName("calculateHoursForLogs - Jornada con almuerzo de 1 hora")
    void calculateHours_withLunchBreak() {
        List<TimeLog> logs = List.of(
                createLog(EventType.INGRESO, LocalDateTime.of(2026, 4, 6, 8, 0)),
                createLog(EventType.INICIO_ALMUERZO, LocalDateTime.of(2026, 4, 6, 12, 0)),
                createLog(EventType.FINAL_ALMUERZO, LocalDateTime.of(2026, 4, 6, 13, 0)),
                createLog(EventType.SALIDA, LocalDateTime.of(2026, 4, 6, 17, 0))
        );

        double hours = payrollService.calculateHoursForLogs(logs);
        assertEquals(8.0, hours);
    }

    @Test
    @DisplayName("calculateHoursForLogs - Lista vacia retorna 0")
    void calculateHours_emptyLogs() {
        double hours = payrollService.calculateHoursForLogs(List.of());
        assertEquals(0.0, hours);
    }

    @Test
    @DisplayName("calculateHoursForLogs - Solo clock-in sin clock-out")
    void calculateHours_onlyClockIn() {
        List<TimeLog> logs = List.of(
                createLog(EventType.INGRESO, LocalDateTime.of(2026, 4, 6, 8, 0))
        );

        double hours = payrollService.calculateHoursForLogs(logs);
        assertEquals(0.0, hours);
    }

    @Test
    @DisplayName("calculateWeeklyPay - 40 horas regulares sin overtime")
    void calculateWeeklyPay_noOvertime() {
        when(timesheetRepository.findByUserAndWorkWeek(any(), any()))
                .thenReturn(Optional.empty());

        // 5 dias x 8 horas = 40 horas
        List<TimeLog> logs = createWeekLogs(5, 8);

        WeeklyPaySummaryDto result = payrollService.calculateWeeklyPay(testWorker, testWeek, logs);

        assertEquals(40.0, result.getTotalHours());
        assertEquals(40.0, result.getRegularHours());
        assertEquals(0.0, result.getOvertimeHours());
        assertEquals(new BigDecimal("600.00"), result.getTotalPay());
    }

    @Test
    @DisplayName("calculateWeeklyPay - Mas de 40 horas activa overtime 1.5x")
    void calculateWeeklyPay_withOvertime() {
        when(timesheetRepository.findByUserAndWorkWeek(any(), any()))
                .thenReturn(Optional.empty());

        // 5 dias x 10 horas = 50 horas (10 overtime)
        List<TimeLog> logs = createWeekLogs(5, 10);

        WeeklyPaySummaryDto result = payrollService.calculateWeeklyPay(testWorker, testWeek, logs);

        assertEquals(50.0, result.getTotalHours());
        assertEquals(40.0, result.getRegularHours());
        assertEquals(10.0, result.getOvertimeHours());
        // Regular: 40 * 15 = 600, Overtime: 10 * 22.50 = 225, Total: 825
        assertEquals(new BigDecimal("825.00"), result.getTotalPay());
    }

    @Test
    @DisplayName("calculateWeeklyPay - 0 horas retorna todo en cero")
    void calculateWeeklyPay_zeroHours() {
        when(timesheetRepository.findByUserAndWorkWeek(any(), any()))
                .thenReturn(Optional.empty());

        WeeklyPaySummaryDto result = payrollService.calculateWeeklyPay(testWorker, testWeek, List.of());

        assertEquals(0.0, result.getTotalHours());
        assertEquals(0.0, result.getRegularHours());
        assertEquals(new BigDecimal("0.00"), result.getTotalPay());
    }

    // --- Helpers ---

    private TimeLog createLog(EventType type, LocalDateTime timestamp) {
        TimeLog log = new TimeLog();
        log.setEventType(type);
        log.setTimestamp(timestamp);
        log.setWorkWeek(testWeek);
        return log;
    }

    private List<TimeLog> createWeekLogs(int days, int hoursPerDay) {
        List<TimeLog> logs = new java.util.ArrayList<>();
        for (int d = 0; d < days; d++) {
            LocalDateTime clockIn = LocalDateTime.of(2026, 4, 6 + d, 8, 0);
            LocalDateTime clockOut = clockIn.plusHours(hoursPerDay);
            logs.add(createLog(EventType.INGRESO, clockIn));
            logs.add(createLog(EventType.SALIDA, clockOut));
        }
        return logs;
    }
}
