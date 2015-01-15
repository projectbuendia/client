package org.msf.records.diagnostics;

/**
 * An enumeration of issues that can be reported by {@link HealthCheck}s.
 */
enum HealthIssue {

    WIFI_DISABLED,

    WIFI_NOT_CONNECTED,

    SERVER_HOST_UNREACHABLE,

    SERVER_NOT_RESPONDING
}
