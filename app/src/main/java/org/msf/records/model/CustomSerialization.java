package org.msf.records.model;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.joda.time.DateTime;

import java.lang.reflect.Type;

/**
 * GSON serializaer for custom types, like Joda DateTime. Largely copied from
 * https://sites.google.com/site/gson/gson-user-guide#TOC-Serializing-and-Deserializing-Generic-Types
 */
public class CustomSerialization {

    public static void registerTo(GsonBuilder gson) {
        gson.registerTypeAdapter(DateTime.class, new DateTimeSerializer());
        gson.registerTypeAdapter(DateTime.class, new DateTimeDeserializer());
    }

    private static class DateTimeSerializer implements JsonSerializer<DateTime> {
        public JsonElement serialize(DateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private static class DateTimeDeserializer implements JsonDeserializer<DateTime> {
        public DateTime deserialize(
                JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new DateTime(json.getAsJsonPrimitive().getAsString());
        }
    }
}
