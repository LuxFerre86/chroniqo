package com.luxferre.chroniqo.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom Logback converter to mask sensitive data in log messages.
 * Masks emails, passwords, and other sensitive patterns.
 */
public class MaskingConverter extends ClassicConverter {

    private static final String EMAIL_PATTERN = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b";
    private static final String PASSWORD_PATTERN = "(?i)(password|pwd|pass)\\s*[:=]\\s*([^\\s]+)";
    private static final String MASK = "***";

    private final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);
    private final Pattern passwordPattern = Pattern.compile(PASSWORD_PATTERN);

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        message = maskEmails(message);
        message = maskPasswords(message);
        return message;
    }

    private String maskEmails(String message) {
        Matcher matcher = emailPattern.matcher(message);
        return matcher.replaceAll(match -> {
            String email = match.group();
            int atIndex = email.indexOf('@');
            if (atIndex > 0) {
                String local = email.substring(0, atIndex);
                String domain = email.substring(atIndex);
                int visibleChars = Math.min(3, local.length());
                return local.substring(0, visibleChars) + MASK + domain;
            }
            return MASK;
        });
    }

    private String maskPasswords(String message) {
        Matcher matcher = passwordPattern.matcher(message);
        return matcher.replaceAll("$1: " + MASK);
    }
}
