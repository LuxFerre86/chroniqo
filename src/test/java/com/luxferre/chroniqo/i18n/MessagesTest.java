package com.luxferre.chroniqo.i18n;

import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class MessagesTest {

    private static Set<String> keys(Locale locale) {
        return new TreeSet<>(ResourceBundle.getBundle("messages", locale,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES)).keySet());
    }

    @Test
    void germanAndEnglishBundlesHaveTheSameKeys() {
        assertThat(keys(Locale.GERMAN)).isEqualTo(keys(Locale.ENGLISH));
    }

    @Test
    void normalizesLanguages() {
        assertThat(Messages.normalizeLanguage("de-AT")).isEqualTo("de");
        assertThat(Messages.normalizeLanguage("DE")).isEqualTo("de");
        assertThat(Messages.normalizeLanguage("fr")).isEqualTo("en");
        assertThat(Messages.normalizeLanguage(null)).isEqualTo("en");
    }

    @Test
    void usesRequestLocaleAndFallsBackToEnglish() {
        assertThat(Messages.get("holiday.default")).isEqualTo("Holiday");
        LocaleContextHolder.setLocale(Locale.GERMAN);
        try {
            assertThat(Messages.get("holiday.default")).isEqualTo("Feiertag");
            assertThat(Messages.get("unit.minutes", 1441)).isEqualTo("1441 Minuten");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
        LocaleContextHolder.setLocale(Locale.FRENCH);
        try {
            assertThat(Messages.get("holiday.default")).isEqualTo("Holiday");
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void forLanguageIgnoresRequestLocale() {
        assertThat(Messages.forLanguage("de", "push.title")).isEqualTo("Zeiterfassung");
        assertThat(Messages.forLanguage("en", "push.title")).isEqualTo("Time tracking");
        assertThat(Messages.forLanguage("de", "mail.verify.body", "Max", "http://x")).contains("Hallo Max");
        assertThat(Messages.forLanguage("en", "mail.reset.body", "Max", "http://x")).contains("didn't request");
    }
}
