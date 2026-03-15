package com.luxferre.chroniqo.model;

/**
 * Lifecycle status of a {@link TimeEntry}.
 *
 * @author Luxferre86
 * @since 22.02.2026
 */
public enum TimeEntryStatus {
    /**
     * Entry has been started - only start time is set
     */
    STARTED,

    /**
     * Entry is complete - start time, end time, and break are set
     */
    COMPLETED
}