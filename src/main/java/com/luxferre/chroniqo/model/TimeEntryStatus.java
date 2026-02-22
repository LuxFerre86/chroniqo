package com.luxferre.chroniqo.model;

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
