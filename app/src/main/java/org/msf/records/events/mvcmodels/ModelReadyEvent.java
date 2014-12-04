package org.msf.records.events.mvcmodels;

/**
 * A sticky event bus event that indicates which parts of the model are ready to be read.
 *
 * <p>This event will only not be posted during first app initialization, when data from the server
 * has never been locally cached.
 */
public class ModelReadyEvent extends ModelEvent {

    public ModelReadyEvent(int... models) {
        super(models);
    }
}
