// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.msf.records.events.mvcmodels;

import android.util.SparseBooleanArray;

/**
 * An abstract event bus event indicating that parts of the model should be read.
 *
 * <p>This class is extended by {@link ModelReadyEvent}, which is a sticky event that always
 * reports every model that is ready to be read; and {@link ModelUpdatedEvent}, which is an event
 * posted whenever the model has changed.
 *
 * @deprecated This class is based on an older revision of the application data model and should be
 *     phased out.
 */
@Deprecated
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
}
