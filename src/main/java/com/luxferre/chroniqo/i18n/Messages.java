package com.luxferre.chroniqo.i18n;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Static lookup of localized, user-facing texts ({@code messages*.properties}).
 *
 * <p>Supported languages are {@code en} (default and fallback) and {@code de}.
 * Without an explicit locale the language of the current request is used
 * (see {@link LocaleContextHolder}); outside a request it is English.
 *
 * @author Luxferre86
 */
public final class Messages {

    public static final String DEFAULT_LANGUAGE = "en";
    public static final List<String> SUPPORTED_LANGUAGES = List.of("en", "de");

    private static final String BUNDLE = "messages";
    private static final ResourceBundle.Control NO_SYSTEM_FALLBACK =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    private Messages() {
    }

    /**
     * Maps an arbitrary language tag or code to a supported language.
     *
     * @param language e.g. {@code de}, {@code de-AT}, {@code fr} or {@code null}
     * @return {@code de} or {@code en}
     */
    public static String normalizeLanguage(String language) {
        if (language == null) {
            return DEFAULT_LANGUAGE;
        }
        String primary = language.trim().toLowerCase(Locale.ROOT).split("[-_]")[0];
        return SUPPORTED_LANGUAGES.contains(primary) ? primary : DEFAULT_LANGUAGE;
    }

    /**
     * Returns the locale of the current request, or English outside a request.
     *
     * @return a supported locale
     */
    public static Locale currentLocale() {
        LocaleContext context = LocaleContextHolder.getLocaleContext();
        Locale locale = context == null ? null : context.getLocale();
        return locale == null ? Locale.ENGLISH : Locale.forLanguageTag(normalizeLanguage(locale.getLanguage()));
    }

    /**
     * Resolves a message for the current request locale.
     *
     * @param key  the message key
     * @param args optional {@link MessageFormat} arguments
     * @return the localized text
     */
    public static String get(String key, Object... args) {
        return get(currentLocale(), key, args);
    }

    /**
     * Resolves a message for the given language (e.g. a user's stored language).
     *
     * @param language language code, normalized to a supported one
     * @param key      the message key
     * @param args     optional {@link MessageFormat} arguments
     * @return the localized text
     */
    public static String forLanguage(String language, String key, Object... args) {
        return get(Locale.forLanguageTag(normalizeLanguage(language)), key, args);
    }

    private static String get(Locale locale, String key, Object... args) {
        String pattern;
        try {
            pattern = ResourceBundle.getBundle(BUNDLE, locale, NO_SYSTEM_FALLBACK).getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        Object[] plain = Arrays.stream(args).map(a -> a instanceof Number ? String.valueOf(a) : a).toArray();
        return new MessageFormat(pattern, locale).format(plain);
    }
}
