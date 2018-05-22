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

import com.google.common.collect.ImmutableSet;

import org.projectbuendia.client.events.diagnostics.TroubleshootingActionsChangedEvent;
import org.projectbuendia.client.utils.Logger;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * Aggregates reported {@link HealthIssue}s and fires events containing the appropriate
 * troubleshooting steps.
 */
public class Troubleshooter {

    private static final Logger LOG = Logger.create();

    private final Object mIssuesLock = new Object();
    private final Object mTroubleshootingLock = new Object();

    private final EventBus mEventBus;
    private Set<HealthIssue> mActiveIssues;

    private TroubleshootingActionsChangedEvent mLastTroubleshootingActionsChangedEvent;

    public Troubleshooter(EventBus eventBus) {
        mEventBus = eventBus;
        mActiveIssues = new HashSet<>();
    }

    /** Returns a set of all currently-active health issues. */
    public ImmutableSet<HealthIssue> getActiveIssues() {
        return ImmutableSet.copyOf(mActiveIssues);
    }

    /**
     * Returns true iff the given issue is current active.
     * @param issue {@link HealthIssue} to check for
     */
    public boolean hasIssue(HealthIssue issue) {
        return mActiveIssues.contains(issue);
    }

    /** Returns true iff no active issues exist. */
    public boolean isHealthy() {
        return mActiveIssues.isEmpty();
    }

    /**
     * Returns true if no ongoing issues preventing access to the Buendia server exist. Note that
     * connectivity is still not guaranteed, just not ruled out.
     */
    public boolean isServerHealthy() {
        return getNetworkConnectivityTroubleshootingActions().isEmpty()
            && getConfigurationTroubleshootingActions().isEmpty();
    }

    private Set<TroubleshootingAction> getNetworkConnectivityTroubleshootingActions() {
        Set<TroubleshootingAction> actions = new HashSet<>();

        if (mActiveIssues.contains(HealthIssue.WIFI_DISABLED)) {
            actions.add(TroubleshootingAction.ENABLE_WIFI);
        } else if (mActiveIssues.contains(HealthIssue.WIFI_NOT_CONNECTED)) {
            actions.add(TroubleshootingAction.CONNECT_WIFI);
        } else if (mActiveIssues.contains(HealthIssue.SERVER_HOST_UNREACHABLE)) {
            actions.add(TroubleshootingAction.CHECK_SERVER_REACHABILITY);
        } else if (mActiveIssues.contains(HealthIssue.SERVER_INTERNAL_ISSUE)) {
            actions.add(TroubleshootingAction.CHECK_SERVER_SETUP);
        } else if (mActiveIssues.contains(HealthIssue.SERVER_NOT_RESPONDING)) {
            actions.add(TroubleshootingAction.CHECK_SERVER_STATUS);
        }

        return actions;
    }

    private Set<TroubleshootingAction> getConfigurationTroubleshootingActions() {
        Set<TroubleshootingAction> actions = new HashSet<>();

        if (mActiveIssues.contains(HealthIssue.SERVER_CONFIGURATION_INVALID)) {
            actions.add(TroubleshootingAction.CHECK_SERVER_CONFIGURATION);
        } else if (mActiveIssues.contains(HealthIssue.SERVER_AUTHENTICATION_ISSUE)) {
            actions.add(TroubleshootingAction.CHECK_SERVER_AUTH);
        }

        return actions;
    }

    /** Called when a new health issue is discovered. */
    public <T extends HealthIssue> void onDiscovered(T healthIssue) {
        synchronized (mIssuesLock) {
            mActiveIssues.add(healthIssue);
        }

        // TODO: Consider scheduling this for ~100 milliseconds in the future so as to
        // prevent multiple troubleshooting events from firing for issues resulting from the same
        // root cause.
        postTroubleshootingEvents(null);
    }

    private void postTroubleshootingEvents(HealthIssue solvedIssue) {
        synchronized (mTroubleshootingLock) {
            ImmutableSet.Builder<TroubleshootingAction> actionsBuilder = ImmutableSet.builder();

            actionsBuilder.addAll(getNetworkConnectivityTroubleshootingActions());
            actionsBuilder.addAll(getConfigurationTroubleshootingActions());

            // NOTE(ping): Disable package server troubleshooting for now. 2018-05-21
            // actionsBuilder.addAll(getPackageServerTroubleshootingActions());

            ImmutableSet<TroubleshootingAction> actions = actionsBuilder.build();

            if (mLastTroubleshootingActionsChangedEvent != null) {
                // If nothing's changed since the last time we checked, don't post a new event.
                if (mLastTroubleshootingActionsChangedEvent.actions.equals(actions)) return;

                mEventBus.removeStickyEvent(mLastTroubleshootingActionsChangedEvent);
            }

            mLastTroubleshootingActionsChangedEvent =
                new TroubleshootingActionsChangedEvent(actions, solvedIssue);
            mEventBus.postSticky(mLastTroubleshootingActionsChangedEvent);
        }
    }

    private Set<TroubleshootingAction> getPackageServerTroubleshootingActions() {
        Set<TroubleshootingAction> actions = new HashSet<>();
        if (mActiveIssues.contains(HealthIssue.PACKAGE_SERVER_HOST_UNREACHABLE)) {
            actions.add(TroubleshootingAction.CHECK_PACKAGE_SERVER_REACHABILITY);
        } else if (mActiveIssues.contains(HealthIssue.PACKAGE_SERVER_INDEX_NOT_FOUND)) {
            actions.add(TroubleshootingAction.CHECK_PACKAGE_SERVER_CONFIGURATION);
        }
        return actions;
    }

    /** Called when a health issue is resolved. */
    public void onResolved(HealthIssue healthIssue) {
        synchronized (mIssuesLock) {
            if (!mActiveIssues.remove(healthIssue)) {
                LOG.w(
                    "Attempted to remove health issue '%1$s' that the troubleshooter was not "
                        + "previously aware of.",
                    healthIssue.toString());
            }
        }

        // TODO: Consider scheduling this for ~100 milliseconds in the future so as to
        // prevent multiple troubleshooting events from firing for issues resulting from the same
        // root cause.
        postTroubleshootingEvents(healthIssue);
    }
}
