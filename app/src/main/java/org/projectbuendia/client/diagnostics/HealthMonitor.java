// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.diagnostics;

import android.app.Application;

import com.google.common.collect.ImmutableSet;

import de.greenrobot.event.EventBus;

/**
 * Monitors the health of the application, collecting issues and passing them on to a
 * {@link Troubleshooter}.
 */
public class HealthMonitor {

    private final Application mApplication;
    private final EventBus mHealthEventBus;
    private final ImmutableSet<HealthCheck> mHealthChecks;
    private final Troubleshooter mTroubleshooter;
    private boolean mRunning = false;

    /** Starts all health checks. */
    public void start() {
        if (!mRunning) {
            mHealthEventBus.register(this);

            for (HealthCheck check : mHealthChecks) {
                check.start(mHealthEventBus);
            }
            mRunning = true;
        }
    }

    /** Stops all health checks. */
    public void stop() {
        if (mRunning) {
            mHealthEventBus.unregister(this);

            for (HealthCheck check : mHealthChecks) {
                check.stop();
            }
            mRunning = false;
        }
    }

    /** Clears all issues for all health checks. */
    public void clear() {
        for (HealthCheck check : mHealthChecks) {
            check.clear();
        }
    }

    public <T extends HealthIssue> void onEvent(HealthIssue.DiscoveredEvent event) {
        mTroubleshooter.onDiscovered(event.getIssue());
    }

    public void onEvent(HealthIssue.ResolvedEvent event) {
        mTroubleshooter.onResolved(event.getIssue());
    }

    /** Returns true if the API is known for certain to be unavailable. */
    public boolean isApiUnavailable() {
        for (HealthCheck check : mHealthChecks) {
            if (check.isApiUnavailable()) {
                return true;
            }
        }
        return false;
    }

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
}
