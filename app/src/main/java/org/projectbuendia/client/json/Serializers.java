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

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.lang.reflect.Type;

/**
 * Gson serializers for custom types like Joda DateTime. Largely copied from
 * https://sites.google.com/site/gson/gson-user-guide#TOC-Serializing-and-Deserializing-Generic-Types
 */
public class Serializers {

    public static void registerTo(GsonBuilder gson) {
        gson.registerTypeAdapter(DateTime.class, new DateTimeSerializer());
        gson.registerTypeAdapter(LocalDate.class, new LocalDateSerializer());
    }
}
