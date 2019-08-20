package org.projectbuendia.client.events.actions;


import java.util.ArrayList;

public class VoidObservationsRequestEvent {
    public final ArrayList<String> uuids;

    public VoidObservationsRequestEvent(ArrayList<String> uuids) {
        this.uuids = uuids;
    }
}
