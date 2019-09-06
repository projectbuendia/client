package org.projectbuendia.client.events.actions;

import java.util.Collection;

public class ObsDeleteRequestedEvent {
    public final Collection<String> uuids;

    public ObsDeleteRequestedEvent(Collection<String> uuids) {
        this.uuids = uuids;
    }
}
