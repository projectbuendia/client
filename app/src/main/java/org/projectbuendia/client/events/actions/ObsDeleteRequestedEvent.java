package org.projectbuendia.client.events.actions;

import org.projectbuendia.client.models.Obs;

import java.util.Collection;

public class ObsDeleteRequestedEvent {
    public final Collection<Obs> observations;

    public ObsDeleteRequestedEvent(Collection<Obs> observations) {
        this.observations = observations;
    }
}
