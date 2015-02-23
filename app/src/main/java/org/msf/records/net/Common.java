package org.msf.records.net;

/**
 * Common constants and helper methods for the network layer.
 */
public final class Common {

    /**
     * The number of milliseconds before a request is considered timed out for requests expected to
     * finish quickly (e.g. updating or deleting a record).
     */
    public static final int REQUEST_TIMEOUT_MS_SHORT = 15000;

    /**
     * The number of milliseconds before a request is considered timed out for requests expected to
     * finish somewhat quickly (e.g. requesting the list of forms from the server).
     */
    public static final int REQUEST_TIMEOUT_MS_MEDIUM = 30000;

    /**
     * The number of milliseconds before a request is considered timed out for requests that may
     * take a considerable amount of time to complete (e.g. requesting all concepts).
     */
    public static final int REQUEST_TIMEOUT_MS_LONG = 60000;

    /**
     * The number of milliseconds before a request is considered timed out for requests that may
     * take an exceedingly long time (e.g. requesting all patient encounters).
     */
    public static final int REQUEST_TIMEOUT_MS_VERY_LONG = 120000;

    private Common() {}
}
