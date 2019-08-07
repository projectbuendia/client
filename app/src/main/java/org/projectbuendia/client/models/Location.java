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

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/** The app model for a location, including its localized name. */
public final @Immutable class Location extends Base<String> implements Comparable<Location> {
    public final @Nonnull String uuid;  // permanent unique identifier
    public final @Nonnull String path;  // short IDs from root to this node, separated by slashes
    public final @Nonnull String name;
    public final int depth;

    /** Creates an instance of {@link Location}. */
    public Location(@Nonnull String uuid, @Nonnull String path, String name) {
        super(null);
        this.uuid = uuid;
        this.path = path;
        this.name = Utils.toNonnull(name);
        this.depth = path.split("/").length;  // split drops trailing empty parts
    }

    @Override public String toString() {
        return Utils.format("<Location %s: %s [%s]>", path, Utils.repr(name), uuid);
    }

    @Override public boolean equals(Object other) {
        return other instanceof Location && uuid.equals(((Location) other).uuid);
    }

    @Override public int hashCode() {
        return Objects.hashCode(uuid);
    }

    public boolean isInSubtree(Location other) {
        return path.startsWith(other.path);
    }

    @Override public int compareTo(Location other) {
        return path.compareTo(other.path);
    }
}
