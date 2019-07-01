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

package org.projectbuendia.client.json;

import com.google.gson.annotations.SerializedName;

import org.projectbuendia.client.utils.LexicographicVersion;

/** A Gson object that represents an available update. */
public class JsonUpdateInfo {

    @SerializedName("url")
    public String url;

    @SerializedName("version")
    public String version;

    /**
     * Returns the parsed {@link LexicographicVersion} or {@code null} if the version is
     * malformed.
     */
    public LexicographicVersion getParsedVersion() {
        try {
            return version != null ? LexicographicVersion.parse(version) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
