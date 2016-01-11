package org.projectbuendia.client.events.actions;


import java.util.ArrayList;

public class VoidObservationsRequestEvent {

    public final ArrayList<String> Uuids;

    public VoidObservationsRequestEvent (ArrayList<String> uuids){
        this.Uuids = uuids;
    }
}
