package com.apv.chronotrack.service;

import com.apv.chronotrack.DTO.ConsolidatedPayrollReportDto;
import com.apv.chronotrack.models.User;
import com.apv.chronotrack.models.WeeklyTimesheet;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${frontend.base.url}")
    private String frontendBaseUrl;

    // @Async so the email send does not block the HTTP response
    @Async
    public void sendRegistrationInvite(String to, String token, String companyName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String registrationUrl = frontendBaseUrl + "/complete-registration?token=" + token;

            String htmlMsg = emailWrapper(
                    "You've been invited to Jornixs",
                    "<h2 style=\"color:#1f2937;margin:0 0 16px 0;font-size:24px;\">Welcome to Jornixs!</h2>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 16px 0;\">You've been invited to join <strong>" + companyName + "</strong>.</p>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 24px 0;\">Click the button below to complete your registration and start tracking your time:</p>"
                  + buttonHtml("Complete Registration", registrationUrl)
                  + "<p style=\"color:#6b7280;font-size:13px;line-height:1.5;margin:24px 0 8px 0;\">If the button doesn't work, copy and paste this URL into your browser:</p>"
                  + "<p style=\"color:#6366f1;font-size:13px;word-break:break-all;margin:0 0 24px 0;\"><a href=\"" + registrationUrl + "\" style=\"color:#6366f1;\">" + registrationUrl + "</a></p>"
                  + "<p style=\"color:#9ca3af;font-size:12px;line-height:1.5;margin:16px 0 0 0;\">If you did not expect this invitation, please ignore this email.</p>"
            );

            helper.setText(htmlMsg, true);
            helper.setTo(to);
            helper.setSubject("You've been invited to join " + companyName + " on Jornixs");
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            log.error("Error sending registration invitation email: {}", e.getMessage(), e);
        }
    }
    @Async
    public void sendCompanyRegistrationInvite(String to, String token) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String registrationUrl = frontendBaseUrl + "/register-company?token=" + token;

            String htmlMsg = emailWrapper(
                    "Welcome to Jornixs",
                    "<h2 style=\"color:#1f2937;margin:0 0 16px 0;font-size:24px;\">Thank you for subscribing to Jornixs!</h2>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 16px 0;\">You're one step away from managing your team's time, attendance and payroll all in one place.</p>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 24px 0;\">Click the button below to finish setting up your company and your administrator account:</p>"
                  + buttonHtml("Complete Company Registration", registrationUrl)
                  + "<p style=\"color:#6b7280;font-size:13px;line-height:1.5;margin:24px 0 8px 0;\">If the button doesn't work, copy and paste this URL into your browser:</p>"
                  + "<p style=\"color:#6366f1;font-size:13px;word-break:break-all;margin:0 0 24px 0;\"><a href=\"" + registrationUrl + "\" style=\"color:#6366f1;\">" + registrationUrl + "</a></p>"
                  + "<p style=\"color:#9ca3af;font-size:12px;line-height:1.5;margin:16px 0 0 0;\">This invitation link is tied to your subscription. If you didn't subscribe to Jornixs, please ignore this email.</p>"
            );

            helper.setText(htmlMsg, true);
            helper.setTo(to);
            helper.setSubject("Welcome to Jornixs — Complete your company setup");
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            log.error("Error sending company registration invite: {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;

            String htmlMsg = emailWrapper(
                    "Reset your Jornixs password",
                    "<h2 style=\"color:#1f2937;margin:0 0 16px 0;font-size:24px;\">Password reset request</h2>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 16px 0;\">We received a request to reset the password for your Jornixs account.</p>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 24px 0;\">Click the button below to choose a new password. This link will expire in <strong>1 hour</strong>.</p>"
                  + buttonHtml("Reset My Password", resetUrl)
                  + "<p style=\"color:#6b7280;font-size:13px;line-height:1.5;margin:24px 0 8px 0;\">If the button doesn't work, copy and paste this URL into your browser:</p>"
                  + "<p style=\"color:#6366f1;font-size:13px;word-break:break-all;margin:0 0 24px 0;\"><a href=\"" + resetUrl + "\" style=\"color:#6366f1;\">" + resetUrl + "</a></p>"
                  + "<div style=\"border-left:4px solid #f59e0b;background:#fef3c7;padding:12px 16px;border-radius:4px;margin:16px 0 0 0;\">"
                  + "<p style=\"color:#92400e;font-size:13px;line-height:1.5;margin:0;\"><strong>Didn't request this?</strong> You can safely ignore this email. Your password will not be changed unless you click the link above.</p>"
                  + "</div>"
            );

            helper.setText(htmlMsg, true);
            helper.setTo(to);
            helper.setSubject("Reset your Jornixs password");
            helper.setFrom(fromEmail);
            mailSender.send(mimeMessage);

        } catch (Exception e) {
            log.error("Error sending password reset email: {}", e.getMessage(), e);
        }
    }

    // --- Email layout helpers (branded, inline-styled for max client compatibility) ---
    private String emailWrapper(String preheader, String content) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>Jornixs</title></head>"
             + "<body style=\"margin:0;padding:0;background:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;\">"
             + "<span style=\"display:none!important;visibility:hidden;opacity:0;height:0;width:0;overflow:hidden;\">" + preheader + "</span>"
             + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background:#f3f4f6;padding:40px 20px;\">"
             + "<tr><td align=\"center\">"
             + "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.05);\">"
             // Header band
             + "<tr><td style=\"background:#1f2937;padding:32px 40px;text-align:center;\">"
             + "<h1 style=\"color:#ffffff;font-size:28px;margin:0;font-weight:800;letter-spacing:-0.5px;\">Jornixs</h1>"
             + "<p style=\"color:#9ca3af;font-size:13px;margin:4px 0 0 0;\">Workforce time tracking made simple</p>"
             + "</td></tr>"
             // Content
             + "<tr><td style=\"padding:40px;\">" + content + "</td></tr>"
             // Footer
             + "<tr><td style=\"background:#f9fafb;padding:24px 40px;text-align:center;border-top:1px solid #e5e7eb;\">"
             + "<p style=\"color:#6b7280;font-size:12px;line-height:1.5;margin:0 0 8px 0;\">&copy; 2026 Jornixs &mdash; APV Solutions LLC</p>"
             + "<p style=\"color:#9ca3af;font-size:12px;line-height:1.5;margin:0;\">Need help? Contact us at <a href=\"mailto:contact@jornixs.com\" style=\"color:#6366f1;text-decoration:none;\">contact@jornixs.com</a></p>"
             + "</td></tr>"
             + "</table>"
             + "</td></tr></table>"
             + "</body></html>";
    }

    private String buttonHtml(String label, String url) {
        return "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"margin:0 auto;\">"
             + "<tr><td align=\"center\" style=\"border-radius:8px;background:#6366f1;\">"
             + "<a href=\"" + url + "\" style=\"display:inline-block;padding:14px 32px;color:#ffffff;font-size:16px;font-weight:600;text-decoration:none;border-radius:8px;\">" + label + "</a>"
             + "</td></tr></table>";
    }

    @Async
    public void sendApprovalNotification(WeeklyTimesheet timesheet) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = emailWrapper(
                    "Your timesheet was approved",
                    "<h2 style=\"color:#1f2937;margin:0 0 16px 0;font-size:24px;\">Good news, " + timesheet.getUser().getFullName() + "!</h2>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 16px 0;\">Your timesheet for the week of <strong>"
                  + timesheet.getWorkWeek().getStartDate() + "</strong> to <strong>" + timesheet.getWorkWeek().getEndDate()
                  + "</strong> has been <strong style=\"color:#059669;\">approved</strong> by your administrator.</p>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 16px 0;\">The corresponding payment will be included in the next payroll cycle.</p>"
            );

            helper.setText(htmlMsg, true);
            helper.setTo(timesheet.getUser().getEmail());
            helper.setSubject("Timesheet Approved — Jornixs");
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            log.error("Error sending approval notification email: {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendRejectionNotification(WeeklyTimesheet timesheet) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = emailWrapper(
                    "Action required: timesheet rejected",
                    "<h2 style=\"color:#1f2937;margin:0 0 16px 0;font-size:24px;\">Hello, " + timesheet.getUser().getFullName() + "</h2>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 16px 0;\">Your timesheet for the week of <strong>"
                  + timesheet.getWorkWeek().getStartDate() + "</strong> to <strong>" + timesheet.getWorkWeek().getEndDate()
                  + "</strong> has been <strong style=\"color:#dc2626;\">rejected</strong> by your administrator.</p>"
                  + "<p style=\"color:#4b5563;font-size:14px;font-weight:600;margin:16px 0 8px 0;\">Reason:</p>"
                  + "<p style=\"font-style:italic;padding:12px 16px;background:#fef2f2;border-left:4px solid #dc2626;border-radius:4px;color:#7f1d1d;font-size:14px;margin:0 0 24px 0;\">"
                  + timesheet.getRejectionReason()
                  + "</p>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 16px 0;\">Please log in to Jornixs, reopen the timesheet to make the necessary corrections, and submit it again for approval.</p>"
            );

            helper.setText(htmlMsg, true);
            helper.setTo(timesheet.getUser().getEmail());
            helper.setSubject("Action Required: Timesheet Rejected — Jornixs");
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            log.error("Error sending rejection notification email: {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendReminderToWorker(User worker) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = emailWrapper(
                    "Reminder: submit your timesheet",
                    "<h2 style=\"color:#1f2937;margin:0 0 16px 0;font-size:24px;\">Hello, " + worker.getFullName() + "</h2>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 16px 0;\">This is a friendly reminder to submit your weekly timesheet for approval.</p>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 24px 0;\">Please log in to Jornixs to complete the process.</p>"
            );

            helper.setText(htmlMsg, true);
            helper.setTo(worker.getEmail());
            helper.setSubject("Reminder: Submit your timesheet — Jornixs");
            helper.setFrom(fromEmail);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Error sending reminder email to worker: {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendAdminWeeklySummary(User admin, ConsolidatedPayrollReportDto report) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = emailWrapper(
                    "Weekly payroll summary",
                    "<h2 style=\"color:#1f2937;margin:0 0 16px 0;font-size:24px;\">Hello, " + admin.getFullName() + "</h2>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 16px 0;\">Here is a summary of the consolidated payroll for the week of <strong>"
                  + report.getStartDate() + "</strong> to <strong>" + report.getEndDate() + "</strong>.</p>"
                  + "<div style=\"background:#f9fafb;border-radius:8px;padding:16px;margin:16px 0;\">"
                  + "<p style=\"color:#6b7280;font-size:14px;margin:0 0 4px 0;\">Total to Pay</p>"
                  + "<p style=\"color:#059669;font-size:28px;font-weight:700;margin:0;\">$" + report.getGrandTotalPay() + "</p>"
                  + "</div>"
                  + "<p style=\"color:#4b5563;font-size:16px;line-height:1.6;margin:0 0 16px 0;\">Please review any pending timesheets in the Jornixs admin panel.</p>"
            );

            helper.setText(htmlMsg, true);
            helper.setTo(admin.getEmail());
            helper.setSubject("Weekly Payroll Summary — Jornixs");
            helper.setFrom(fromEmail);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Error sending weekly admin summary email: {}", e.getMessage(), e);
        }
    }
}