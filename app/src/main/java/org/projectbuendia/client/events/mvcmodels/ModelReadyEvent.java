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

package org.projectbuendia.client.events.mvcmodels;

/**
 * A sticky event bus event that indicates which parts of the model are ready to be read.
 *
 * <p>This event will only not be posted during first app initialization, when data from the server
 * has never been locally cached.
 *
 * @deprecated This class is based on an older revision of the application data model and should be
 *     phased out.
 */
@Deprecated
public class ModelReadyEvent extends ModelEvent {

    public ModelReadyEvent(int... models) {
        super(models);
    }
}
