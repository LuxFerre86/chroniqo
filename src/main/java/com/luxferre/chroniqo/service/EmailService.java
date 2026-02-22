package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send email verification
     */
    public void sendVerificationEmail(User user) {
        String verificationLink = baseUrl + "/verify-email?token=" + user.getVerificationToken();

        String subject = "ChroniQo - Verify your email";
        String message = String.format("""
                Hello %s,
                
                Welcome to ChroniQo! Please verify your email address by clicking the link below:
                
                %s
                
                This link will expire in 24 hours.
                
                If you didn't create an account, please ignore this email.
                
                Best regards,
                ChroniQo Team
                """, user.getFirstName(), verificationLink);

        sendEmail(user.getEmail(), subject, message);
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(User user) {
        String resetLink = baseUrl + "/reset-password-confirm?token=" + user.getResetToken();

        String subject = "ChroniQo - Password Reset";
        String message = String.format("""
                Hello %s,
                
                You requested to reset your password. Click the link below to set a new password:
                
                %s
                
                This link will expire in 1 hour.
                
                If you didn't request this, please ignore this email and your password will remain unchanged.
                
                Best regards,
                ChroniQo Team
                """, user.getFirstName(), resetLink);

        sendEmail(user.getEmail(), subject, message);
    }

    /**
     * Send generic email
     */
    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
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
