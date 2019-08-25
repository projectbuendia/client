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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/** The app model for a location, including its localized name. */
public final @Immutable class Location extends Model {
    public @Nonnull String name;

    /** Creates an instance of {@link Location}. */
    public Location(@Nonnull String uuid, String name) {
        super(uuid);
        this.name = Utils.toNonnull(name);
    }

    @Override public String toString() {
        return Utils.format("<Location %s [%s]>", Utils.repr(name), uuid);
    }
}
