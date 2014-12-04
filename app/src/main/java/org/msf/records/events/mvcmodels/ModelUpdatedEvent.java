package org.msf.records.events.mvcmodels;

/**
 * An event bus event indicating that the model has been updated.
 */
public class ModelUpdatedEvent extends ModelEvent {

    public ModelUpdatedEvent(int... models) {
        super(models);
    }
}
