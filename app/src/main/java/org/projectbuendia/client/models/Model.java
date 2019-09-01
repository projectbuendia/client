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

package org.projectbuendia.client.models;

import org.projectbuendia.client.utils.Utils;

import static org.projectbuendia.client.utils.Utils.eq;

/** An abstract base class for application models, which all have UUIDs. */
public abstract class Model {
    public final String uuid;

    protected Model(String uuid) {
        this.uuid = Utils.toNonnull(uuid);
    }

    public boolean equals(Object other) {
        return other instanceof Model && eq(uuid, ((Model) other).uuid);
    }

    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }

    public String toString() {
        return "<" + getClass().getSimpleName() + " " + uuid + ">";
    }
}
