package com.apv.chronotrack.service;

import com.apv.chronotrack.DTO.TimesheetSummaryDto;
import com.apv.chronotrack.models.*;
import com.apv.chronotrack.repository.UserRepository;
import com.apv.chronotrack.repository.WeeklyTimesheetRepository;
import com.apv.chronotrack.repository.WorkWeekRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimesheetService {

    private final WeeklyTimesheetRepository timesheetRepository;
    private final WorkWeekRepository workWeekRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    /**
     * El trabajador envía su hoja de horas para aprobación.
     */
    @Transactional
    public void submitTimesheet(User user, Long workWeekId) {
        User freshUser = findFreshUser(user);
        WorkWeek workWeek = workWeekRepository.findById(workWeekId)
                .orElseThrow(() -> new EntityNotFoundException("Work week not found."));

        WeeklyTimesheet timesheet = timesheetRepository.findByUserAndWorkWeek(freshUser, workWeek)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet not found for this week."));

        if (timesheet.getStatus() != TimesheetStatus.OPEN) {
            throw new IllegalStateException("This timesheet cannot be submitted because it is not in 'Open' status.");
        }
        timesheet.setStatus(TimesheetStatus.SUBMITTED);
        timesheet.setSubmittedAt(LocalDateTime.now());

        auditService.logAction(
                user,
                user.getCompany(),
                "TIMESHEET_SUBMITTED",
                WeeklyTimesheet.class.getSimpleName(),
                timesheet.getId(),
                "Worker submitted their timesheet."
        );

        timesheetRepository.save(timesheet);
    }

    /**
     * El administrador aprueba una hoja de horas.
     */
    @Transactional
    public void approveTimesheet(Long timesheetId, User admin) {
        User freshAdmin = findFreshUser(admin);
        WeeklyTimesheet timesheet = findTimesheetAndVerifyCompany(timesheetId, freshAdmin);
        WeeklyTimesheet savedTimesheet = timesheetRepository.save(timesheet);
        if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted timesheets can be approved.");
        }
        auditService.logAction(
                admin,
                admin.getCompany(),
                "TIMESHEET_APPROVED",
                WeeklyTimesheet.class.getSimpleName(),
                savedTimesheet.getId(),
                "Timesheet approved for " + savedTimesheet.getUser().getFullName()
        );
        timesheet.setStatus(TimesheetStatus.APPROVED);
        timesheet.setApprovedAt(LocalDateTime.now());
        emailService.sendApprovalNotification(savedTimesheet);
        timesheetRepository.save(timesheet);
    }

    /**
     * El administrador rechaza una hoja de horas.
     */
    @Transactional
    public void rejectTimesheet(Long timesheetId, String reason, User admin) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A reason is required to reject the timesheet.");
        }

        User freshAdmin = findFreshUser(admin);
        WeeklyTimesheet timesheet = findTimesheetAndVerifyCompany(timesheetId, freshAdmin);

        if (timesheet.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted timesheets can be rejected.");
        }

        timesheet.setStatus(TimesheetStatus.REJECTED);
        WeeklyTimesheet savedTimesheet = timesheetRepository.save(timesheet);
        timesheet.setRejectionReason(reason);

        auditService.logAction(
                admin,
                admin.getCompany(),
                "TIMESHEET_REJECTED",
                WeeklyTimesheet.class.getSimpleName(),
                savedTimesheet.getId(),
                "Timesheet rejected for: " + savedTimesheet.getUser().getFullName() + ". Reason: " + reason
        );
        emailService.sendRejectionNotification(savedTimesheet);
        timesheetRepository.save(timesheet);
    }

    // --- Métodos Auxiliares ---

    private User findFreshUser(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }

    private WeeklyTimesheet findTimesheetAndVerifyCompany(Long timesheetId, User admin) {
        WeeklyTimesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet not found with ID: " + timesheetId));

        // Security check: admin and the timesheet's worker must belong to the same company
        if (!timesheet.getUser().getCompany().getId().equals(admin.getCompany().getId())) {
            throw new SecurityException("Access denied: you do not have permission to manage this timesheet.");
        }
        return timesheet;
    }

    @Transactional(readOnly = true)
    public List<TimesheetSummaryDto> getSubmittedTimesheets(User admin) {
        User freshAdmin = findFreshUser(admin);
        List<WeeklyTimesheet> timesheets = timesheetRepository
                .findByUser_CompanyAndStatus(freshAdmin.getCompany(), TimesheetStatus.SUBMITTED);

        return timesheets.stream().map(ts -> TimesheetSummaryDto.builder()
                        .timesheetId(ts.getId())
                        .workerId(ts.getUser().getId())
                        .workerName(ts.getUser().getFullName())
                        .workWeekId(ts.getWorkWeek().getId())
                        .weekStartDate(ts.getWorkWeek().getStartDate())
                        .weekEndDate(ts.getWorkWeek().getEndDate())
                        .status(ts.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimesheetSummaryDto> getFilteredTimesheets(User currentUser, TimesheetStatus status, Long workerId) {
        User freshUser = findFreshUser(currentUser);

        // Si el que consulta es un trabajador, solo puede ver sus propias hojas de horas
        if (freshUser.getRole().getRoleName() == RoleName.ROLE_TRABAJADOR) {
            workerId = freshUser.getId();
        }

        List<WeeklyTimesheet> timesheets = timesheetRepository
                .findFilteredTimesheets(freshUser.getCompany(), status, workerId);

        return timesheets.stream().map(ts -> TimesheetSummaryDto.builder()
                        .timesheetId(ts.getId())
                        .workerId(ts.getUser().getId())
                        .workerName(ts.getUser().getFullName())
                        .workWeekId(ts.getWorkWeek().getId())
                        .weekStartDate(ts.getWorkWeek().getStartDate())
                        .weekEndDate(ts.getWorkWeek().getEndDate())
                        .status(ts.getStatus())
                        .rejectionReason(ts.getRejectionReason())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TimesheetSummaryDto> getFilteredTimesheetsPaged(User currentUser, TimesheetStatus status, Long workerId, Pageable pageable) {
        User freshUser = findFreshUser(currentUser);

        if (freshUser.getRole().getRoleName() == RoleName.ROLE_TRABAJADOR) {
            workerId = freshUser.getId();
        }

        Page<WeeklyTimesheet> timesheets = timesheetRepository
                .findFilteredTimesheetsPaged(freshUser.getCompany(), status, workerId, pageable);

        return timesheets.map(ts -> TimesheetSummaryDto.builder()
                .timesheetId(ts.getId())
                .workerId(ts.getUser().getId())
                .workerName(ts.getUser().getFullName())
                .workWeekId(ts.getWorkWeek().getId())
                .weekStartDate(ts.getWorkWeek().getStartDate())
                .weekEndDate(ts.getWorkWeek().getEndDate())
                .status(ts.getStatus())
                .rejectionReason(ts.getRejectionReason())
                .build());
    }

    // --- MÉTODO NUEVO PARA REENVIAR ---
    @Transactional
    public void resubmitTimesheet(User user, Long timesheetId) {
        User freshUser = findFreshUser(user);
        WeeklyTimesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet not found."));

        // Security check: a worker can only resubmit their own timesheets
        if (!timesheet.getUser().getId().equals(freshUser.getId())) {
            throw new SecurityException("You do not have permission to modify this timesheet.");
        }

        if (timesheet.getStatus() != TimesheetStatus.REJECTED) {
            throw new IllegalStateException("Only rejected timesheets can be resubmitted.");
        }

        // Se cambia el estado de vuelta a ABIERTA para que el trabajador pueda editarla,
        // y se limpia el motivo del rechazo.
        timesheet.setStatus(TimesheetStatus.OPEN);
        timesheet.setRejectionReason(null);

        auditService.logAction(
                user,
                user.getCompany(),
                "TIMESHEET_REOPENED",
                WeeklyTimesheet.class.getSimpleName(),
                timesheet.getId(),
                "Worker reopened a rejected timesheet."
        );
        timesheetRepository.save(timesheet);
    }
}