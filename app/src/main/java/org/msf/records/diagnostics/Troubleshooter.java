package org.msf.records.diagnostics;

import com.google.common.collect.ImmutableSet;

import org.msf.records.events.diagnostics.TroubleshootingNotRequiredEvent;
import org.msf.records.events.diagnostics.TroubleshootingRequiredEvent;
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

    private static final TroubleshootingNotRequiredEvent TROUBLESHOOTING_NOT_REQUIRED_EVENT =
            new TroubleshootingNotRequiredEvent();

    private final Object mIssuesLock = new Object();
    private final Object mTroubleshootingLock = new Object();

    private final EventBus mEventBus;
    private Set<HealthIssue> mActiveIssues;

    private TroubleshootingRequiredEvent mLastTroubleshootingRequiredEvent;

    public Troubleshooter(EventBus eventBus) {
        mEventBus = eventBus;
        mActiveIssues = new HashSet<>();
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
        troubleshoot();
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
        troubleshoot();
    }

    private void troubleshoot() {
        synchronized (mTroubleshootingLock) {
            ImmutableSet.Builder<TroubleshootingAction> actionsBuilder = ImmutableSet.builder();

            actionsBuilder.addAll(troubleshootNetworkConnectivity());
            actionsBuilder.addAll(troubleshootConfiguration());

            ImmutableSet actions = actionsBuilder.build();

            if (!actions.isEmpty()) {
                mEventBus.removeStickyEvent(TROUBLESHOOTING_NOT_REQUIRED_EVENT);

                mLastTroubleshootingRequiredEvent = new TroubleshootingRequiredEvent(actions);
                mEventBus.postSticky(mLastTroubleshootingRequiredEvent);
            } else {
                if (mLastTroubleshootingRequiredEvent != null) {
                    mEventBus.removeStickyEvent(mLastTroubleshootingRequiredEvent);
                    mLastTroubleshootingRequiredEvent = null;
                }

                mEventBus.postSticky(new TroubleshootingNotRequiredEvent());
            }
        }
    }

    private Set<TroubleshootingAction> troubleshootNetworkConnectivity() {
        Set<TroubleshootingAction> actions = new HashSet<>();

        if (mActiveIssues.contains(HealthIssue.WIFI_DISABLED)) {
            actions.add(TroubleshootingAction.ENABLE_WIFI);
        } else if (mActiveIssues.contains(HealthIssue.WIFI_NOT_CONNECTED)) {
            actions.add(TroubleshootingAction.CONNECT_WIFI);
        } else if (mActiveIssues.contains(HealthIssue.SERVER_HOST_UNREACHABLE)) {
            actions.add(TroubleshootingAction.CHECK_SERVER_REACHABILITY);
        } else if (mActiveIssues.contains(HealthIssue.SERVER_NOT_RESPONDING)) {
            actions.add(TroubleshootingAction.CHECK_SERVER_STATUS);
        }

        return actions;
    }

    private Set<TroubleshootingAction> troubleshootConfiguration() {
        Set<TroubleshootingAction> actions = new HashSet<>();

        if (mActiveIssues.contains(HealthIssue.SERVER_CONFIGURATION_INVALID)) {
            actions.add(TroubleshootingAction.CHECK_SERVER_CONFIGURATION);
        }

        return actions;
    }
}
