package org.projectbuendia.client.events.actions;


import java.util.ArrayList;

public class ObsDeleteRequestedEvent {
    public final ArrayList<String> uuids;

    public ObsDeleteRequestedEvent(ArrayList<String> uuids) {
        this.uuids = uuids;
    }
}
