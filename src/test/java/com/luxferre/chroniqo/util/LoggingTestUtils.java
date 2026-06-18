package com.luxferre.chroniqo.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class for asserting log output in tests.
 *
 * <p>Provides methods to capture and verify log messages at specific levels
 * without relying on log output files or external systems. Uses Logback's
 * ListAppender to capture log events during test execution.
 *
 * <p>Usage:
 * <pre>
 *   LoggingTestUtils logs = LoggingTestUtils.captureLogsFor(MyService.class);
 *   // ... execute code ...
 *   logs.assertContains(Level.INFO, "Expected message");
 * </pre>
 *
 * @author Test Helper
 * @since 01.06.2026
 */
public class LoggingTestUtils {

    private final ListAppender<ILoggingEvent> listAppender;
    private final Logger logger;

    private LoggingTestUtils(Class<?> clazz) {
        this.logger = (Logger) LoggerFactory.getLogger(clazz);
        this.listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    /**
     * Creates a LoggingTestUtils instance that captures logs for the given class.
     *
     * @param clazz the class whose logs should be captured
     * @return a LoggingTestUtils instance configured for log capture
     */
    public static LoggingTestUtils captureLogsFor(Class<?> clazz) {
        return new LoggingTestUtils(clazz);
    }

    /**
     * Asserts that at least one log message at the given level contains the expected text.
     *
     * @param level    the log level to check (INFO, WARN, ERROR, etc.)
     * @param expected the text that should be contained in a log message
     */
    public void assertContains(Level level, String expected) {
        List<String> messages = getMessagesAtLevel(level);
        assertThat(messages)
                .anySatisfy(msg -> assertThat(msg).contains(expected));
    }

    /**
     * Asserts that exactly the expected number of log messages at the given level exist.
     *
     * @param level    the log level to check
     * @param expected the expected number of messages
     */
    public void assertCount(Level level, int expected) {
        List<String> messages = getMessagesAtLevel(level);
        assertThat(messages).hasSize(expected);
    }

    /**
     * Asserts that at least one log message at the given level contains all the expected texts.
     *
     * @param level    the log level to check
     * @param expected the texts that should all be contained in a log message
     */
    public void assertContainsAll(Level level, String... expected) {
        List<String> messages = getMessagesAtLevel(level);
        assertThat(messages)
                .anySatisfy(msg -> assertThat(msg).contains(expected));
    }

    /**
     * Asserts that no log messages at the given level contain the specified text.
     *
     * @param level      the log level to check
     * @param unexpected the text that should NOT be contained in any log message
     */
    public void assertNotContains(Level level, String unexpected) {
        List<String> messages = getMessagesAtLevel(level);
        assertThat(messages)
                .noneMatch(msg -> msg.contains(unexpected));
    }

    /**
     * Asserts that sensitive data (emails, passwords) has been masked in logs.
     * Verifies that no plain email addresses or password values are present.
     */
    public void assertSensitiveDataMasked() {
        List<String> allMessages = listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());

        for (String msg : allMessages) {
            // Check that no plain email patterns exist (simplified check)
            assertThat(msg)
                    .doesNotMatch(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*");
            // Check that no password values are logged (excluding "password=" patterns)
            assertThat(msg)
                    .doesNotMatch("(?i)password\\s*[=:]\\s*(?!\\(masked\\))\\S+");
        }
    }

    /**
     * Clears all captured log messages.
     */
    public void clearLogs() {
        listAppender.list.clear();
    }

    /**
     * Returns all log messages at the specified level.
     *
     * @param level the log level to filter by
     * @return list of formatted log messages at the given level
     */
    public List<String> getMessagesAtLevel(Level level) {
        return listAppender.list.stream()
                .filter(event -> event.getLevel() == level)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    /**
     * Returns all captured log messages at all levels.
     *
     * @return list of all formatted log messages
     */
    public List<String> getAllMessages() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    /**
     * Removes the ListAppender from the logger. Should be called in a @AfterEach method.
     */
    public void stop() {
        logger.detachAppender(listAppender);
        listAppender.stop();
    }
}

