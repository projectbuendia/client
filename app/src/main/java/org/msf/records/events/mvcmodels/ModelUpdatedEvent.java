package org.msf.records.events.mvcmodels;

import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

/**
 * An event bus event indicating that the model has been updated.
 */
public class ModelUpdatedEvent extends ModelEvent {

    public ModelUpdatedEvent(int... models) {
        super(models);
    }
}
