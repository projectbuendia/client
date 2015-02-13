package org.msf.records.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import org.joda.time.DateTime;

import java.lang.reflect.Type;

/**
 * Utility class for serializing JODA DateTime objects from JSON returned by the OpenMRS Buendia
 * module.
 */
public class DateTimeDeserializer implements JsonDeserializer<DateTime> {
    @Override
    public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return DateTime.parse(json.getAsString());
    }
}
