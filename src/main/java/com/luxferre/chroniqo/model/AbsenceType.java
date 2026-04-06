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
     * Planned leave approved in advance \(e.g., holiday\).
     */
    VACATION,

    /**
     * Unplanned leave caused by illness.
     */
    SICK
}