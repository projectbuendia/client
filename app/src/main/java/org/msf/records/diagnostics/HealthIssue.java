package org.msf.records.diagnostics;

/**
 * An enumeration of issues that can be reported by {@link HealthCheck}s.
 *
 * <p>A {@link HealthIssue} is an ongoing issue with the application (rather than a one-time
 * problematic event): when a new issue is discovered, it will be reported to an instance of
 * {@link HealthMonitor}, which will consider that issue active until the issue is resolved.
 */
enum HealthIssue {

    WIFI_DISABLED,

    WIFI_NOT_CONNECTED,

    SERVER_AUTHENTICATION_ISSUE,

    SERVER_CONFIGURATION_INVALID,

    SERVER_HOST_UNREACHABLE,

    SERVER_INTERNAL_ISSUE,

    SERVER_NOT_RESPONDING;

    /**
     * The event to be posted when a health issue is discovered.
     */
    public final DiscoveredEvent discovered = new DiscoveredEvent();

    /**
     * The event to be posted when a health issue is resolved.
     */
    public final ResolvedEvent resolved = new ResolvedEvent();

    class Event {

        public HealthIssue getIssue() {
            return HealthIssue.this;
        }

        private Event() {}
    }

    class DiscoveredEvent extends Event {}

    class ResolvedEvent extends Event {}
}
