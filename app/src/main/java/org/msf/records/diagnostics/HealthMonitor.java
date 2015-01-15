package org.msf.records.diagnostics;

import android.app.Application;

import com.google.common.collect.ImmutableSet;

import de.greenrobot.event.EventBus;

/**
 * An object that monitors the health of the application, collecting issues and passing them on to
 * a {@link Troubleshooter}.
 */
public class HealthMonitor {

    private final Application mApplication;
    private final EventBus mHealthEventBus;
    private final ImmutableSet<HealthCheck> mHealthChecks;
    private final Troubleshooter mTroubleshooter;

    HealthMonitor(
            Application application,
            EventBus healthEventBus,
            ImmutableSet<HealthCheck> healthChecks,
            Troubleshooter troubleshooter) {
        mApplication = application;
        mHealthEventBus = healthEventBus;
        mHealthChecks = healthChecks;
        mTroubleshooter = troubleshooter;
    }

    /**
     * Starts all health checks.
     */
    public void start() {
        mHealthEventBus.register(this);

        for (HealthCheck check : mHealthChecks) {
            check.start(mHealthEventBus);
        }
    }

    /**
     * Stops all health checks.
     */
    public void stop() {
        mHealthEventBus.unregister(this);

        for (HealthCheck check : mHealthChecks) {
            check.stop();
        }
    }

    public <T extends HealthIssue> void onEvent(HealthIssueDiscoveredEvent event) {
        mTroubleshooter.onDiscovered(event.healthIssue);
    }

    public void onEvent(HealthIssueResolvedEvent event) {
        mTroubleshooter.onResolved(event.healthIssue);
    }
}
