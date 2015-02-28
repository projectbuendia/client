package org.msf.records.events.diagnostics;

import com.google.common.collect.ImmutableSet;

import org.msf.records.diagnostics.TroubleshootingAction;

/**
 * An event bus event indicating that the set of troubleshooting actions required has changed.
 */
public class TroubleshootingActionsChangedEvent {

    public final ImmutableSet<TroubleshootingAction> actions;

    public TroubleshootingActionsChangedEvent(ImmutableSet<TroubleshootingAction> actions) {
        this.actions = actions;
    }
}
