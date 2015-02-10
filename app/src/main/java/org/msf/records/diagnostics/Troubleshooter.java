package org.msf.records.diagnostics;

import com.google.common.collect.ImmutableSet;

import org.msf.records.events.diagnostics.TroubleshootingActionsChangedEvent;
import org.msf.records.utils.Logger;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * An object that aggregates reported {@link HealthIssue}s and fires events containing the
 * appropriate troubleshooting steps.
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

    /**
     * Returns a set of all currently-active health issues.
     */
    public ImmutableSet<HealthIssue> getActiveIssues() {
        return ImmutableSet.copyOf(mActiveIssues);
    }

    /**
     * Returns true iff no active issues exist.
     */
    public boolean isHealthy() {
        return mActiveIssues.isEmpty();
    }

    /**
     * Called when a new health issue is discovered.
     */
    public <T extends HealthIssue> void onDiscovered(T healthIssue) {
        synchronized (mIssuesLock) {
            mActiveIssues.add(healthIssue);
        }

        // TODO(dxchen): Consider scheduling this for ~100 milliseconds in the future so as to
        // prevent multiple troubleshooting events from firing for issues resulting from the same
        // root cause.
        postTroubleshootingEvents();
    }

    /**
     * Called when a health issue is resolved.
     */
    public void onResolved(HealthIssue healthIssue) {
        synchronized (mIssuesLock) {
            if (!mActiveIssues.remove(healthIssue)) {
                LOG.w(
                        "Attempted to remove health issue '%1$s' that the troubleshooter was not "
                                + "previously aware of.",
                        healthIssue.toString());
            }
        }

        // TODO(dxchen): Consider scheduling this for ~100 milliseconds in the future so as to
        // prevent multiple troubleshooting events from firing for issues resulting from the same
        // root cause.
        postTroubleshootingEvents();
    }

    private void postTroubleshootingEvents() {
        synchronized (mTroubleshootingLock) {
            ImmutableSet.Builder<TroubleshootingAction> actionsBuilder = ImmutableSet.builder();

            actionsBuilder.addAll(getNetworkConnectivityTroubleshootingActions());
            actionsBuilder.addAll(getConfigurationTroubleshootingActions());

            ImmutableSet actions = actionsBuilder.build();

            if (mLastTroubleshootingActionsChangedEvent != null) {
                // If nothing's changed since the last time we checked, don't post a new event.
                if (mLastTroubleshootingActionsChangedEvent.actions.equals(actions)) {
                    return;
                }

                mEventBus.removeStickyEvent(mLastTroubleshootingActionsChangedEvent);
            }

            mLastTroubleshootingActionsChangedEvent =
                    new TroubleshootingActionsChangedEvent(actions);
            mEventBus.postSticky(mLastTroubleshootingActionsChangedEvent);
        }
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
}
