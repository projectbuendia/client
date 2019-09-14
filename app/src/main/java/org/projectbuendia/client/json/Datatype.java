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

import javax.annotation.Nullable;

/** OpenMRS concept types (used for JSON representation and elsewhere). */
public enum Datatype {
    // These serialized names must match the HL7_TYPE_NAMES on the server side.
    @SerializedName("numeric") NUMERIC,
    @SerializedName("boolean") BOOLEAN,
    @SerializedName("coded") CODED,
    @SerializedName("text") TEXT,
    @SerializedName("date") DATE,
    @SerializedName("datetime") DATETIME,
    @SerializedName("none") NONE;

    /** Converts a Sex value to a code for JSON communication with the server. */
    public static @Nullable String serialize(@Nullable Datatype type) {
        if (type == null) return null;
        try {
            return Datatype.class.getField(type.name()).getAnnotation(SerializedName.class).value();
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    public static @Nullable Datatype deserialize(@Nullable String name) {
        if (name == null) return null;
        return Datatype.valueOf(name);
    }
}
