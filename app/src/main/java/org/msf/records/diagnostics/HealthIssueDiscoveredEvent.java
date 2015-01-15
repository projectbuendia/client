package org.msf.records.diagnostics;

/**
 * An event bus event that indicates a new health issue has been discovered.
 */
public class HealthIssueDiscoveredEvent {

    public final HealthIssue healthIssue;

    public HealthIssueDiscoveredEvent(HealthIssue healthIssue) {
        this.healthIssue = healthIssue;
    }
}
