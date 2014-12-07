package org.msf.records.events.mvcmodels;

import android.util.SparseBooleanArray;

/**
 * An abstract event bus event indicating that parts of the model should be read.
 *
 * <p>This class is extended by {@link ModelReadyEvent}, which is a sticky event that always
 * reports every model that is ready to be read; and {@link ModelUpdatedEvent}, which is an event
 * posted whenever the model has changed.
 */
public abstract class ModelEvent {

    protected final SparseBooleanArray mModels = new SparseBooleanArray();

    /**
     * Creates a new {@link ModelEvent}.
     *
     * @param models the models that should be read
     */
    protected ModelEvent(int... models) {
        for (int model : models) {
            mModels.append(model, true);
        }
    }

    /**
     * Returns whether the specified model should .
     */
    public boolean shouldRead(int model) {
        return mModels.get(model, false);
    }
}
