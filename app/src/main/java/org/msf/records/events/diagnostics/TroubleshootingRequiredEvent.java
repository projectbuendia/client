package org.msf.records.events.diagnostics;

import com.google.common.collect.ImmutableSet;

import org.msf.records.diagnostics.TroubleshootingAction;

/**
 * An event bus event indicating that the application has some health issues and troubleshooting is
 * required.
 */
public class TroubleshootingRequiredEvent {

    public final ImmutableSet<TroubleshootingAction> actions;

    public TroubleshootingRequiredEvent(ImmutableSet<TroubleshootingAction> actions) {
        this.actions = actions;
    }
}
