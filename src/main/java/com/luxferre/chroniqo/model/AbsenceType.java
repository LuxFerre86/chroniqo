package com.luxferre.chroniqo.model;

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
    VACATION("Vacation", false, false),

    /**
     * Unplanned leave caused by illness. Drops the daily target.
     */
    SICK("Sick", false, false),

    /**
     * Day off taken to reduce overtime. The daily target stays, nothing is
     * worked, so the time balance drops by the daily target.
     */
    FLEX_DAY("Flextime day", true, false),

    /**
     * Parental leave. Drops the daily target.
     */
    PARENTAL_LEAVE("Parental leave", false, false),

    /**
     * Business trip. The daily target is credited as worked time, so the
     * balance stays neutral.
     */
    BUSINESS_TRIP("Business trip", true, true),

    /**
     * Special leave (e.g., wedding or moving). Drops the daily target and does
     * not count against vacation days.
     */
    SPECIAL_LEAVE("Special leave", false, false);

    private final String label;
    private final boolean keepsTarget;
    private final boolean creditsTarget;

    AbsenceType(String label, boolean keepsTarget, boolean creditsTarget) {
        this.label = label;
        this.keepsTarget = keepsTarget;
        this.creditsTarget = creditsTarget;
    }

    /** @return human-readable name shown in the UI */
    public String getLabel() {
        return label;
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
