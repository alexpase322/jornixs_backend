package com.apv.chronotrack.service;

import com.apv.chronotrack.DTO.ConsolidatedPayrollReportDto;
import com.apv.chronotrack.models.*;
import com.apv.chronotrack.repository.CompanyRepository;
import com.apv.chronotrack.repository.UserRepository;
import com.apv.chronotrack.repository.WeeklyTimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationSchedulerService {

    private final WeeklyTimesheetRepository timesheetRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AdminService adminService;

    /**
     * Envía un recordatorio a los trabajadores cuya hoja de horas de la semana
     * pasada sigue 'ABIERTA'.
     * Se ejecuta todos los lunes a las 8 AM.
     */
    @Scheduled(cron = "0 0 8 * * MON") // 08:00 cada lunes
    @Transactional(readOnly = true)
    public void sendWorkerReminders() {
        System.out.println("Ejecutando tarea: Enviando recordatorios a trabajadores...");

        List<Company> companies = companyRepository.findAll();
        for (Company company : companies) {
            // Buscamos las hojas de horas de la semana pasada que siguen abiertas
            List<WeeklyTimesheet> openTimesheets = timesheetRepository
                    .findByUser_CompanyAndStatus(company, TimesheetStatus.OPEN);

            for (WeeklyTimesheet ts : openTimesheets) {
                // Verificamos si la semana de la hoja de horas ya terminó
                if (ts.getWorkWeek().getEndDate().isBefore(LocalDate.now())) {
                    emailService.sendReminderToWorker(ts.getUser());
                }
            }
        }
        System.out.println("Tarea de recordatorios finalizada.");
    }

    /**
     * Envía un resumen de la nómina de la semana anterior a todos los administradores.
     * Se ejecuta todos los lunes a las 9 AM.
     */
    @Scheduled(cron = "0 0 9 * * MON") // 09:00 cada lunes
    @Transactional(readOnly = true)
    public void sendAdminSummaries() {
        System.out.println("Ejecutando tarea: Enviando resúmenes a administradores...");
        LocalDate today = LocalDate.now();
        LocalDate startOfLastWeek = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfLastWeek = today.minusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<Company> companies = companyRepository.findAll();
        for (Company company : companies) {
            // Buscamos a todos los administradores de esta compañía
            List<User> admins = userRepository.findByCompany(company).stream()
                    .filter(user -> user.getRole().getRoleName() == RoleName.ROLE_ADMINISTRADOR)
                    .collect(Collectors.toList());

            if (!admins.isEmpty()) {
                // Usamos el primer admin para generar el reporte (todos los admins de la misma compañía verán lo mismo)
                User representativeAdmin = admins.get(0);

                // Reutilizamos nuestro servicio de reportes para generar el consolidado de la semana pasada
                ConsolidatedPayrollReportDto report = adminService.generateConsolidatedPayrollReport(startOfLastWeek, endOfLastWeek, representativeAdmin);

                // Enviamos el resumen a cada administrador
                for (User admin : admins) {
                    emailService.sendAdminWeeklySummary(admin, report);
                }
            }
        }
        System.out.println("Tarea de resúmenes finalizada.");
    }
}