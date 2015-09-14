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

package org.projectbuendia.client.net.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.joda.time.LocalDate;

import java.lang.reflect.Type;

/** Provides custom Gson serialization for {@link LocalDate} objects. */
public class LocalDateSerializer implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

    @Override public JsonElement serialize(LocalDate date, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(date.toString());
    }

    @Override public LocalDate deserialize(JsonElement json, Type type, JsonDeserializationContext context)
        throws JsonParseException {
        String text = json.getAsString();
        try {
            return LocalDate.parse(text);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException(e);
        }
    }
}
