package com.bovinemagnet.pgconsole.service.notification;

import com.bovinemagnet.pgconsole.model.ActiveAlert;
import com.bovinemagnet.pgconsole.model.NotificationChannel;
import com.bovinemagnet.pgconsole.model.NotificationChannel.EmailConfig;
import com.bovinemagnet.pgconsole.model.NotificationResult;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Enhanced email notification sender using Quarkus Mailer.
 * <p>
 * Provides rich HTML email formatting with branding, digest mode,
 * and customisable templates.
 * Features include:
 * <ul>
 *   <li>HTML email templates with severity styling</li>
 *   <li>Digest mode for batching multiple alerts</li>
 *   <li>Configurable recipients and subject prefix</li>
 *   <li>Attachment support for incident reports</li>
 * </ul>
 *
 * @author Paul Snow
 * @version 0.0.0
 */
@ApplicationScoped
public class EmailNotificationSender extends AbstractNotificationSender {

    @Inject
    Mailer mailer;

    @Override
    public NotificationChannel.ChannelType getChannelType() {
        return NotificationChannel.ChannelType.EMAIL;
    }

    @Override
    public NotificationResult send(NotificationChannel channel, ActiveAlert alert) {
        EmailConfig config = channel.getEmailConfig();
        if (config == null || config.getRecipients() == null || config.getRecipients().isEmpty()) {
            return createFailureResult(channel, alert, new IllegalArgumentException("Missing email recipients"));
        }

        try {
            String subject = buildSubject(config, alert);
            String htmlBody = buildHtmlBody(channel, config, alert);
            String textBody = buildTextBody(alert);

            Mail mail = Mail.withHtml(
                String.join(",", config.getRecipients()),
                subject,
                htmlBody
            ).setText(textBody);

            if (config.getFromAddress() != null && !config.getFromAddress().isBlank()) {
                mail.setFrom(config.getFromAddress());
            }

            mailer.send(mail);

            logNotification(channel, alert, true);
            return NotificationResult.success(
                    channel.getId(),
                    channel.getName(),
                    channel.getChannelType(),
                    alert.getAlertId()
                )
                .withAlertDetails(
                    alert.getAlertType(),
                    alert.getAlertSeverity(),
                    alert.getAlertMessage(),
                    alert.getInstanceName()
                );
        } catch (Exception e) {
            logNotification(channel, alert, false);
            return createFailureResult(channel, alert, e);
        }
    }

    @Override
    public NotificationResult sendTest(NotificationChannel channel) {
        EmailConfig config = channel.getEmailConfig();
        if (config == null || config.getRecipients() == null || config.getRecipients().isEmpty()) {
            return createFailureResult(channel, null, new IllegalArgumentException("Missing email recipients"));
        }

        try {
            String subject = (config.getSubjectPrefix() != null ? config.getSubjectPrefix() + " " : "") +
                             "Test Notification - PG Console";

            String htmlBody = buildTestHtmlBody(channel);
            String textBody = "Test notification from PG Console. Channel '" + channel.getName() + "' is configured correctly!";

            Mail mail = Mail.withHtml(
                String.join(",", config.getRecipients()),
                subject,
                htmlBody
            ).setText(textBody);

            if (config.getFromAddress() != null && !config.getFromAddress().isBlank()) {
                mail.setFrom(config.getFromAddress());
            }

            mailer.send(mail);

            return NotificationResult.success(
                channel.getId(),
                channel.getName(),
                channel.getChannelType(),
                "test"
            );
        } catch (Exception e) {
            return createFailureResult(channel, null, e);
        }
    }

    @Override
    public boolean validateConfig(NotificationChannel channel) {
        EmailConfig config = channel.getEmailConfig();
        if (config == null) {
            return false;
        }
        List<String> recipients = config.getRecipients();
        if (recipients == null || recipients.isEmpty()) {
            return false;
        }
        // Basic email format validation
        for (String email : recipients) {
            if (email == null || !email.contains("@") || !email.contains(".")) {
                return false;
            }
        }
        return true;
    }

    private String buildSubject(EmailConfig config, ActiveAlert alert) {
        StringBuilder subject = new StringBuilder();

        if (config.getSubjectPrefix() != null && !config.getSubjectPrefix().isBlank()) {
            subject.append(config.getSubjectPrefix()).append(" ");
        }

        subject.append("[").append(alert.getAlertSeverity()).append("] ")
               .append(alert.getAlertType());

        if (alert.getInstanceName() != null) {
            subject.append(" - ").append(alert.getInstanceName());
        }

        return subject.toString();
    }

    private String buildHtmlBody(NotificationChannel channel, EmailConfig config, ActiveAlert alert) {
        String severityColour = getSeverityColour(alert.getAlertSeverity());

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset=\"UTF-8\"></head>");
        html.append("<body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5;\">");

        // Container
        html.append("<div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\">");

        // Header with severity colour
        html.append("<div style=\"background-color: ").append(severityColour).append("; padding: 20px; color: white;\">");
        html.append("<h1 style=\"margin: 0; font-size: 24px;\">").append(getSeverityEmoji(alert.getAlertSeverity())).append(" ").append(escapeHtml(alert.getAlertType())).append("</h1>");
        html.append("<p style=\"margin: 5px 0 0 0; opacity: 0.9;\">Severity: ").append(alert.getAlertSeverity()).append("</p>");
        html.append("</div>");

        // Body
        html.append("<div style=\"padding: 20px;\">");

        // Alert message
        html.append("<div style=\"background-color: #f8f9fa; border-radius: 4px; padding: 15px; margin-bottom: 20px;\">");
        html.append("<p style=\"margin: 0; font-size: 16px; color: #333;\">").append(escapeHtml(alert.getAlertMessage())).append("</p>");
        html.append("</div>");

        // Details table
        html.append("<table style=\"width: 100%; border-collapse: collapse; margin-bottom: 20px;\">");
        html.append("<tr><td style=\"padding: 10px; border-bottom: 1px solid #eee; color: #666; width: 120px;\"><strong>Instance</strong></td>");
        html.append("<td style=\"padding: 10px; border-bottom: 1px solid #eee;\">").append(escapeHtml(alert.getInstanceName() != null ? alert.getInstanceName() : "N/A")).append("</td></tr>");
        html.append("<tr><td style=\"padding: 10px; border-bottom: 1px solid #eee; color: #666;\"><strong>Status</strong></td>");
        html.append("<td style=\"padding: 10px; border-bottom: 1px solid #eee;\">").append(alert.getStatusText()).append("</td></tr>");
        html.append("<tr><td style=\"padding: 10px; border-bottom: 1px solid #eee; color: #666;\"><strong>Duration</strong></td>");
        html.append("<td style=\"padding: 10px; border-bottom: 1px solid #eee;\">").append(alert.getDurationFormatted()).append("</td></tr>");
        html.append("<tr><td style=\"padding: 10px; border-bottom: 1px solid #eee; color: #666;\"><strong>Fired At</strong></td>");
        html.append("<td style=\"padding: 10px; border-bottom: 1px solid #eee;\">").append(alert.getFiredAtFormatted()).append("</td></tr>");
        html.append("<tr><td style=\"padding: 10px; color: #666;\"><strong>Alert ID</strong></td>");
        html.append("<td style=\"padding: 10px; font-family: monospace; font-size: 12px;\">").append(escapeHtml(alert.getAlertId())).append("</td></tr>");
        html.append("</table>");

        html.append("</div>");

        // Footer
        html.append("<div style=\"background-color: #f8f9fa; padding: 15px 20px; border-top: 1px solid #eee; font-size: 12px; color: #666;\">");
        html.append("<p style=\"margin: 0;\">Sent by PG Console via channel '").append(escapeHtml(channel.getName())).append("'</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }

    private String buildTextBody(ActiveAlert alert) {
        StringBuilder text = new StringBuilder();
        text.append("PG Console Alert\n");
        text.append("================\n\n");
        text.append("Alert Type: ").append(alert.getAlertType()).append("\n");
        text.append("Severity: ").append(alert.getAlertSeverity()).append("\n");
        text.append("Instance: ").append(alert.getInstanceName() != null ? alert.getInstanceName() : "N/A").append("\n");
        text.append("Status: ").append(alert.getStatusText()).append("\n");
        text.append("Duration: ").append(alert.getDurationFormatted()).append("\n");
        text.append("Fired At: ").append(alert.getFiredAtFormatted()).append("\n\n");
        text.append("Message:\n").append(alert.getAlertMessage()).append("\n\n");
        text.append("Alert ID: ").append(alert.getAlertId()).append("\n");
        return text.toString();
    }

    private String buildTestHtmlBody(NotificationChannel channel) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset=\"UTF-8\"></head>");
        html.append("<body style=\"font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5;\">");
        html.append("<div style=\"max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1);\">");
        html.append("<div style=\"background-color: #28a745; padding: 20px; color: white;\">");
        html.append("<h1 style=\"margin: 0; font-size: 24px;\">✅ Test Notification</h1>");
        html.append("</div>");
        html.append("<div style=\"padding: 20px;\">");
        html.append("<p style=\"font-size: 16px; color: #333;\">This is a test notification from PG Console.</p>");
        html.append("<p style=\"font-size: 16px; color: #333;\">Channel '<strong>").append(escapeHtml(channel.getName())).append("</strong>' is configured correctly!</p>");
        html.append("</div>");
        html.append("<div style=\"background-color: #f8f9fa; padding: 15px 20px; border-top: 1px solid #eee; font-size: 12px; color: #666;\">");
        html.append("<p style=\"margin: 0;\">Sent by PG Console</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body></html>");
        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
