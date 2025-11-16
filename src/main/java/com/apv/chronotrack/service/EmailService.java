package com.apv.chronotrack.service;

import com.apv.chronotrack.DTO.ConsolidatedPayrollReportDto;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.models.WeeklyTimesheet;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    @Async // Hace que el envío de correo no bloquee la respuesta HTTP
    public void sendRegistrationInvite(String to, String token, String companyName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String registrationUrl = frontendBaseUrl + "/complete-registration?token=" + token;

            String htmlMsg = "<h3>¡Bienvenido a ChronoTrack!</h3>"
                    + "<p>Has sido invitado a unirte a <strong>" + companyName + "</strong>.</p>"
                    + "<p>Por favor, haz clic en el siguiente enlace para completar tu registro:</p>"
                    + "<a href=\"" + registrationUrl + "\">Completar Registro</a>"
                    + "<p>Si no puedes hacer clic en el enlace, copia y pega esta URL en tu navegador:</p>"
                    + "<p>" + registrationUrl + "</p>"
                    + "<br>"
                    + "<p>Gracias,<br>El equipo de ChronoTrack</p>";

            helper.setText(htmlMsg, true); // true indica que el contenido es HTML
            helper.setTo(to);
            helper.setSubject("Invitación para unirte a ChronoTrack");
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            // En una aplicación real, aquí manejarías el error de forma más robusta (logs, reintentos, etc.)
            System.err.println("Error al enviar el correo: " + e.getMessage());
        }
    }
    @Async
    public void sendCompanyRegistrationInvite(String to, String token) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // URL correcta para el registro de la COMPAÑÍA
            String registrationUrl = frontendBaseUrl + "/register-company?token=" + token;

            // Texto del correo adaptado para el nuevo administrador
            String htmlMsg = "<h3>¡Gracias por suscribirte a ChronoTrack!</h3>"
                    + "<p>Estás a un solo paso de empezar a gestionar tu equipo.</p>"
                    + "<p>Por favor, haz clic en el siguiente enlace para registrar tu compañía y tu cuenta de administrador:</p>"
                    + "<a href=\"" + registrationUrl + "\">Registrar mi Compañía</a>"
                    + "<p>Si no puedes hacer clic en el enlace, copia y pega esta URL en tu navegador:</p>"
                    + "<p>" + registrationUrl + "</p>"
                    + "<br>"
                    + "<p>Gracias,<br>El equipo de ChronoTrack</p>";

            helper.setText(htmlMsg, true);
            helper.setTo(to);
            helper.setSubject("Completa tu registro en ChronoTrack");
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            System.err.println("Error al enviar el correo de registro de compañía: " + e.getMessage());
        }
    }
    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;

            String htmlMsg = "<h3>Restablecer Contraseña de ChronoTrack</h3>"
                    + "<p>Hemos recibido una solicitud para restablecer tu contraseña. Haz clic en el siguiente enlace para continuar:</p>"
                    + "<a href=\"" + resetUrl + "\">Restablecer mi Contraseña</a>"
                    + "<p>Si no solicitaste esto, puedes ignorar este correo. El enlace expirará en 1 hora.</p>";

            helper.setText(htmlMsg, true);
            helper.setTo(to);
            helper.setSubject("Solicitud de Restablecimiento de Contraseña");
            helper.setFrom(fromEmail);
            mailSender.send(mimeMessage);

        } catch (Exception e) {
            System.err.println("Error al enviar el correo de reseteo de contraseña: " + e.getMessage());
        }
    }

    // En EmailService.java

    @Async
    public void sendApprovalNotification(WeeklyTimesheet timesheet) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String subject = "Tu hoja de horas ha sido APROBADA";
            String htmlMsg = "<h3>¡Buenas noticias, " + timesheet.getUser().getFullName() + "!</h3>"
                    + "<p>Tu hoja de horas para la semana del <strong>"
                    + timesheet.getWorkWeek().getStartDate() + "</strong> al <strong>" + timesheet.getWorkWeek().getEndDate()
                    + "</strong> ha sido aprobada por tu administrador.</p>"
                    + "<p>El pago correspondiente se incluirá en el próximo ciclo de nómina.</p>"
                    + "<br>"
                    + "<p>Gracias,<br>El equipo de ChronoTrack</p>";

            helper.setText(htmlMsg, true);
            helper.setTo(timesheet.getUser().getEmail());
            helper.setSubject(subject);
            helper.setFrom(fromEmail); // 'fromEmail' es la variable que tienes en tu clase con el email remitente

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            System.err.println("Error al enviar el correo de aprobación: " + e.getMessage());
        }
    }

    @Async
    public void sendRejectionNotification(WeeklyTimesheet timesheet) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String subject = "Acción Requerida: Tu hoja de horas ha sido RECHAZADA";
            String htmlMsg = "<h3>Hola, " + timesheet.getUser().getFullName() + ".</h3>"
                    + "<p>Tu hoja de horas para la semana del <strong>"
                    + timesheet.getWorkWeek().getStartDate() + "</strong> al <strong>" + timesheet.getWorkWeek().getEndDate()
                    + "</strong> ha sido rechazada por tu administrador.</p>"
                    + "<p><b>Motivo del rechazo:</b></p>"
                    + "<p style=\"font-style: italic; padding: 10px; background-color: #f0f0f0; border-left: 4px solid #d9534f;\">"
                    + timesheet.getRejectionReason()
                    + "</p>"
                    + "<p>Por favor, ingresa a ChronoTrack, reabre la hoja de horas para hacer las correcciones necesarias y vuelve a enviarla para su aprobación.</p>";

            helper.setText(htmlMsg, true);
            helper.setTo(timesheet.getUser().getEmail());
            helper.setSubject(subject);
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            System.err.println("Error al enviar el correo de rechazo: " + e.getMessage());
        }
    }

    @Async
    public void sendReminderToWorker(User worker) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String subject = "Recordatorio: Envía tu hoja de horas";
            String htmlMsg = "<h3>Hola, " + worker.getFullName() + ".</h3>"
                    + "<p>Este es un recordatorio amistoso para que envíes tu hoja de horas de la semana para su aprobación.</p>"
                    + "<p>Por favor, ingresa a ChronoTrack para completar el proceso.</p>";

            helper.setText(htmlMsg, true);
            helper.setTo(worker.getEmail());
            helper.setSubject(subject);
            helper.setFrom(fromEmail);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Error al enviar el correo de recordatorio al trabajador: " + e.getMessage());
        }
    }

    @Async
    public void sendAdminWeeklySummary(User admin, ConsolidatedPayrollReportDto report) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String subject = "Resumen Semanal de Nómina - ChronoTrack";
            String htmlMsg = "<h3>Hola, " + admin.getFullName() + ".</h3>"
                    + "<p>Adjunto encontrarás un resumen de la nómina consolidada para la semana del <strong>"
                    + report.getStartDate() + "</strong> al <strong>" + report.getEndDate() + "</strong>.</p>"
                    + "<p>Total a Pagar: <strong>$" + report.getGrandTotalPay() + "</strong></p>"
                    + "<p>Por favor, revisa las hojas de horas pendientes de aprobación en el panel de ChronoTrack.</p>";

            helper.setText(htmlMsg, true);
            helper.setTo(admin.getEmail());
            helper.setSubject(subject);
            helper.setFrom(fromEmail);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            System.err.println("Error al enviar el resumen semanal al admin: " + e.getMessage());
        }
    }
}