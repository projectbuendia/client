package org.msf.records.diagnostics;

/**
 * An enumeration of issues that can be reported by {@link HealthCheck}s.
 */
enum HealthIssue {

    WIFI_DISABLED,

    WIFI_NOT_CONNECTED,

    SERVER_CONFIGURATION_INVALID,

    SERVER_HOST_UNREACHABLE,

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
