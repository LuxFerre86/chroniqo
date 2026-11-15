package com.luxferre.chroniqo.model;

import com.luxferre.chroniqo.i18n.Messages;
/**
 * Represents the supported absence categories for a working day entry.
 * Used to distinguish leave reasons in time tracking and reporting.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
public enum AbsenceType {

    /**
     * Planned leave approved in advance (e.g., holiday). Drops the daily target.
     */
    VACATION(false, false),

    /**
     * Unplanned leave caused by illness. Drops the daily target.
     */
    SICK(false, false),

    /**
     * Day off taken to reduce overtime. The daily target stays, nothing is
     * worked, so the time balance drops by the daily target.
     */
    FLEX_DAY(true, false),

    /**
     * Parental leave. Drops the daily target.
     */
    PARENTAL_LEAVE(false, false),

    /**
     * Business trip. The daily target is credited as worked time, so the
     * balance stays neutral.
     */
    BUSINESS_TRIP(true, true),

    /**
     * Special leave (e.g., wedding or moving). Drops the daily target and does
     * not count against vacation days.
     */
    SPECIAL_LEAVE(false, false);

    private final boolean keepsTarget;
    private final boolean creditsTarget;

    AbsenceType(boolean keepsTarget, boolean creditsTarget) {
        this.keepsTarget = keepsTarget;
        this.creditsTarget = creditsTarget;
    }

    /** @return human-readable name shown in the UI */
    public String getLabel() {
        return Messages.get("absence." + name().toLowerCase(java.util.Locale.ROOT));
    }

    /** @return {@code true} if the daily target still applies on such a day */
    public boolean keepsTarget() {
        return keepsTarget;
    }

    /** @return {@code true} if the daily target is credited as worked time */
    public boolean creditsTarget() {
        return creditsTarget;
    }
}
