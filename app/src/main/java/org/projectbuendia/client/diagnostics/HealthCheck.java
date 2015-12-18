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
import android.content.BroadcastReceiver;
import android.support.annotation.Nullable;

import org.projectbuendia.client.utils.Logger;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * An individual health check to be performed while the application is active.
 * <p/>
 * <p>Subclasses can choose to implement this class however they see fit so long as they call
 * {@link #reportIssue} when a new issue occurs and {@link #resolveIssue} or
 * {@link #resolveAllIssues} when issues are resolved. For example, if a health change can be
 * triggered by an Android OS broadcast, it may register a {@link BroadcastReceiver}
 * in {@link #startImpl}; if it needs to poll another service, it may start a background thread.
 * <p/>
 * <p>Subclasses must stop checking (e.g., unregister {@link BroadcastReceiver}s or stop
 * background threads) when {@link #stopImpl} is called.
 */
public abstract class HealthCheck {

    private static final Logger LOG = Logger.create();

    private final Object mLock = new Object();

    protected final Application mApplication;
    protected final HashSet<HealthIssue> mActiveIssues;

    @Nullable private EventBus mHealthEventBus;

    /**
     * Starts the health check.
     * <p/>
     * <p>After this method is called, the health check must post any health issue events on the
     * specified {@link EventBus}.
     */
    public final void start(EventBus healthEventBus) {
        synchronized (mLock) {
            mHealthEventBus = healthEventBus;
            startImpl();
        }
    }

    protected abstract void startImpl();

    /**
     * Stops the health check without clearing its issues.
     * <p/>
     * <p>{@link #start} may be called again to restart checks.
     */
    public final void stop() {
        synchronized (mLock) {
            mHealthEventBus = null;
            stopImpl();
        }
    }

    protected abstract void stopImpl();

    /** Clears all the issues for this health check. */
    public final void clear() {
        mActiveIssues.clear();
    }

    /**
     * Returns true if this HealthCheck knows for certain that the Buendia
     * API is unavailable at this moment.  Implementations of this method
     * should never return true unless they can guarantee that their knowledge
     * of the system state is up to date; for example, if a HealthCheck decides
     * to return true when the network is down, it is responsible for detecting
     * any event that could cause the network to come back up.
     */
    public boolean isApiUnavailable() {
        return false;
    }

    protected HealthCheck(Application application) {
        mApplication = application;
        mActiveIssues = new HashSet<>();
    }

    /** Reports an issue as being active. */
    protected final void reportIssue(HealthIssue healthIssue) {
        EventBus eventBus;
        synchronized (mLock) {
            if (mHealthEventBus == null) {
                LOG.w(
                    "A health issue was reported even though no event bus was registered to "
                        + "handle it: %1$s.",
                    healthIssue.toString());
                return;
            }

            mActiveIssues.add(healthIssue);
            eventBus = mHealthEventBus;
        }

        eventBus.post(healthIssue.discovered);
    }

    /** Marks as resolved all issues that are currently active. */
    protected final void resolveAllIssues() {
        EventBus eventBus;
        Set<HealthIssue> activeIssues;
        synchronized (mLock) {
            if (mHealthEventBus == null) {
                LOG.w(
                    "Health issues were resolved even though no event bus was registered to "
                        + "handle them.");
                return;
            }

            eventBus = mHealthEventBus;
            activeIssues = new HashSet<>(mActiveIssues);
            mActiveIssues.clear();
        }

        for (HealthIssue healthIssue : activeIssues) {
            eventBus.post(healthIssue.resolved);
        }
    }

    /**
     * Marks as resolved the specified issue.
     * <p/>
     * <p>If the issue was not previously reported, this method does nothing.
     */
    protected final void resolveIssue(HealthIssue healthIssue) {
        EventBus eventBus;
        boolean wasIssueActive;
        synchronized (mLock) {
            if (mHealthEventBus == null) {
                LOG.w(
                    "A health issue was resolved even though no event bus was registered to "
                        + "handle it: %1$s.",
                    healthIssue.toString());
                return;
            }

            eventBus = mHealthEventBus;
            wasIssueActive = mActiveIssues.remove(healthIssue);
        }

        if (wasIssueActive) {
            eventBus.post(healthIssue.resolved);
        }
    }

    protected final boolean hasIssue(HealthIssue healthIssue) {
        for (HealthIssue issue : mActiveIssues) {
            if (issue.equals(healthIssue)){
                return true;
            }
        }
        return false;
    }
}
