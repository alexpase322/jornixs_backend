package com.apv.chronotrack.service;

import com.apv.chronotrack.models.*;
import com.apv.chronotrack.repository.UserRepository;
import com.apv.chronotrack.repository.WeeklyTimesheetRepository;
import com.apv.chronotrack.repository.WorkWeekRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceTest {

    @Mock private WeeklyTimesheetRepository timesheetRepository;
    @Mock private WorkWeekRepository workWeekRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private AuditService auditService;

    @InjectMocks
    private TimesheetService timesheetService;

    private User testWorker;
    private User testAdmin;
    private Company testCompany;
    private WorkWeek testWeek;
    private WeeklyTimesheet testTimesheet;

    @BeforeEach
    void setUp() {
        testCompany = new Company();
        testCompany.setId(1L);
        testCompany.setCompanyName("Test Company");

        Role workerRole = new Role();
        workerRole.setRoleName(RoleName.ROLE_TRABAJADOR);

        Role adminRole = new Role();
        adminRole.setRoleName(RoleName.ROLE_ADMINISTRADOR);

        testWorker = new User();
        testWorker.setId(1L);
        testWorker.setFullName("Test Worker");
        testWorker.setEmail("worker@test.com");
        testWorker.setCompany(testCompany);
        testWorker.setRole(workerRole);
        testWorker.setHourlyRate(new BigDecimal("15.00"));

        testAdmin = new User();
        testAdmin.setId(2L);
        testAdmin.setFullName("Test Admin");
        testAdmin.setEmail("admin@test.com");
        testAdmin.setCompany(testCompany);
        testAdmin.setRole(adminRole);

        testWeek = new WorkWeek();
        testWeek.setId(1L);
        testWeek.setStartDate(LocalDate.of(2026, 4, 6));
        testWeek.setEndDate(LocalDate.of(2026, 4, 12));

        testTimesheet = new WeeklyTimesheet();
        testTimesheet.setId(1L);
        testTimesheet.setUser(testWorker);
        testTimesheet.setWorkWeek(testWeek);
        testTimesheet.setStatus(TimesheetStatus.OPEN);
    }

    @Test
    @DisplayName("submitTimesheet - Exito cuando estado es OPEN")
    void submitTimesheet_success() {
        when(userRepository.findById(testWorker.getId())).thenReturn(Optional.of(testWorker));
        when(workWeekRepository.findById(testWeek.getId())).thenReturn(Optional.of(testWeek));
        when(timesheetRepository.findByUserAndWorkWeek(testWorker, testWeek)).thenReturn(Optional.of(testTimesheet));

        timesheetService.submitTimesheet(testWorker, testWeek.getId());

        assertEquals(TimesheetStatus.SUBMITTED, testTimesheet.getStatus());
        assertNotNull(testTimesheet.getSubmittedAt());
        verify(timesheetRepository).save(testTimesheet);
    }

    @Test
    @DisplayName("submitTimesheet - Falla cuando estado no es OPEN")
    void submitTimesheet_failsWhenNotOpen() {
        testTimesheet.setStatus(TimesheetStatus.SUBMITTED);

        when(userRepository.findById(testWorker.getId())).thenReturn(Optional.of(testWorker));
        when(workWeekRepository.findById(testWeek.getId())).thenReturn(Optional.of(testWeek));
        when(timesheetRepository.findByUserAndWorkWeek(testWorker, testWeek)).thenReturn(Optional.of(testTimesheet));

        assertThrows(IllegalStateException.class, () ->
                timesheetService.submitTimesheet(testWorker, testWeek.getId())
        );
    }

    @Test
    @DisplayName("approveTimesheet - Exito cuando estado es SUBMITTED")
    void approveTimesheet_success() {
        testTimesheet.setStatus(TimesheetStatus.SUBMITTED);

        when(userRepository.findById(testAdmin.getId())).thenReturn(Optional.of(testAdmin));
        when(timesheetRepository.findById(testTimesheet.getId())).thenReturn(Optional.of(testTimesheet));
        when(timesheetRepository.save(any())).thenReturn(testTimesheet);

        timesheetService.approveTimesheet(testTimesheet.getId(), testAdmin);

        assertEquals(TimesheetStatus.APPROVED, testTimesheet.getStatus());
        verify(emailService).sendApprovalNotification(any());
    }

    @Test
    @DisplayName("rejectTimesheet - Falla sin motivo")
    void rejectTimesheet_failsWithoutReason() {
        assertThrows(IllegalArgumentException.class, () ->
                timesheetService.rejectTimesheet(testTimesheet.getId(), "", testAdmin)
        );
    }

    @Test
    @DisplayName("rejectTimesheet - Falla cuando estado no es SUBMITTED")
    void rejectTimesheet_failsWhenNotSubmitted() {
        testTimesheet.setStatus(TimesheetStatus.OPEN);

        when(userRepository.findById(testAdmin.getId())).thenReturn(Optional.of(testAdmin));
        when(timesheetRepository.findById(testTimesheet.getId())).thenReturn(Optional.of(testTimesheet));

        assertThrows(IllegalStateException.class, () ->
                timesheetService.rejectTimesheet(testTimesheet.getId(), "Error en horas", testAdmin)
        );
    }

    @Test
    @DisplayName("resubmitTimesheet - Exito cuando estado es REJECTED")
    void resubmitTimesheet_success() {
        testTimesheet.setStatus(TimesheetStatus.REJECTED);
        testTimesheet.setRejectionReason("Horas incorrectas");

        when(userRepository.findById(testWorker.getId())).thenReturn(Optional.of(testWorker));
        when(timesheetRepository.findById(testTimesheet.getId())).thenReturn(Optional.of(testTimesheet));

        timesheetService.resubmitTimesheet(testWorker, testTimesheet.getId());

        assertEquals(TimesheetStatus.OPEN, testTimesheet.getStatus());
        assertNull(testTimesheet.getRejectionReason());
    }

    @Test
    @DisplayName("resubmitTimesheet - Falla cuando estado no es REJECTED")
    void resubmitTimesheet_failsWhenNotRejected() {
        testTimesheet.setStatus(TimesheetStatus.APPROVED);

        when(userRepository.findById(testWorker.getId())).thenReturn(Optional.of(testWorker));
        when(timesheetRepository.findById(testTimesheet.getId())).thenReturn(Optional.of(testTimesheet));

        assertThrows(IllegalStateException.class, () ->
                timesheetService.resubmitTimesheet(testWorker, testTimesheet.getId())
        );
    }

    @Test
    @DisplayName("submitTimesheet - Falla cuando semana no existe")
    void submitTimesheet_failsWhenWeekNotFound() {
        when(userRepository.findById(testWorker.getId())).thenReturn(Optional.of(testWorker));
        when(workWeekRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                timesheetService.submitTimesheet(testWorker, 999L)
        );
    }
}
