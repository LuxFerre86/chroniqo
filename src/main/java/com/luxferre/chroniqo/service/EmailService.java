package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.i18n.Messages;
import com.luxferre.chroniqo.config.AppProperties;
import com.luxferre.chroniqo.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending transactional emails (email verification and
 * password reset).
 *
 * <p>All outbound mail is sent via the configured JavaMailSender (Gmail SMTP).
 * Delivery failures are caught and logged; they do not propagate to callers so
 * that a temporary mail-server issue cannot break registration or password-reset
 * flows entirely. Sensitive data like email addresses are automatically masked
 * in log output via Logback configuration.
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final MailProperties mailProperties;

    /**
     * Send email verification
     */
    public void sendVerificationEmail(User user) {
        String verificationLink = appProperties.getBaseUrl() + "/verify-email?token=" + user.getVerificationToken();

        String lang = user.getLanguage();
        String subject = Messages.forLanguage(lang, "mail.verify.subject");
        String message = Messages.forLanguage(lang, "mail.verify.body", user.getFirstName(), verificationLink);

        sendEmail(user.getEmail(), subject, message);
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(User user) {
        String resetLink = appProperties.getBaseUrl() + "/reset-password-confirm?token=" + user.getResetToken();

        String lang = user.getLanguage();
        String subject = Messages.forLanguage(lang, "mail.reset.subject");
        String message = Messages.forLanguage(lang, "mail.reset.body", user.getFirstName(), resetLink);

        sendEmail(user.getEmail(), subject, message);
    }

    /**
     * Send generic email
     */
    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getUsername());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            // In production: maybe queue for retry
        }
    }
}