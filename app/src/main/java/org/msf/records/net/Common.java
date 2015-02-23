package org.msf.records.net;

/**
 * Common constants and helper methods for the network layer.
 */
public final class Common {

    /**
     * The number of seconds before a request is considered timed out for requests expected to
     * finish quickly (e.g. updating or deleting a record).
     */
    public static final int REQUEST_TIMEOUT_SECS_SHORT = 15;

    /**
     * The number of seconds before a request is considered timed out for requests expected to
     * finish somewhat quickly (e.g. requesting the list of forms from the server).
     */
    public static final int REQUEST_TIMEOUT_SECS_MEDIUM = 30;

    /**
     * The number of seconds before a request is considered timed out for requests that may take
     * a considerable amount of time to complete (e.g. requesting all concepts).
     */
    public static final int REQUEST_TIMEOUT_SECS_LONG = 60;

    /**
     * The number of seconds before a request is considered timed out for requests that may take
     * an exceedingly long time (e.g. requesting all patient encounters).
     */
    public static final int REQUEST_TIMEOUT_SECS_VERY_LONG = 120;

    private Common() {}
}
