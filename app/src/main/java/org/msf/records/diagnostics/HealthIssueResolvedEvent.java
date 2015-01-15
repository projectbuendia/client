package org.msf.records.diagnostics;

/**
 * An event bus event that indicates an existing health issue has been resolved.
 */
public class HealthIssueResolvedEvent {

    public final HealthIssue healthIssue;

    public HealthIssueResolvedEvent(HealthIssue healthIssue) {
        this.healthIssue = healthIssue;
    }
}
