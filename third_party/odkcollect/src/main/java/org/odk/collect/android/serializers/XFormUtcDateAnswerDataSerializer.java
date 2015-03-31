package org.odk.collect.android.serializers;

import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.xform.util.XFormAnswerDataSerializer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * An {@link XFormAnswerDataSerializer} that correctly formats dates in the RFC3339 format. The
 * default XFormAnswerDataSerializer has logic to perform this task, but seems to improperly
 * determine the client's time zone offset. This class overrides this default behavior to ensure
 * that the local time is correctly represented by always representing timestamps in UTC, accounting
 * for the local time zone offset.
 */
public class XFormUtcDateAnswerDataSerializer extends XFormAnswerDataSerializer {
    private static final DateFormat RFC3339_UTC_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    static {
        RFC3339_UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public Object serializeAnswerData(DateTimeData data) {
        return RFC3339_UTC_FORMAT.format((Date)data.getValue());
    }

    // TODO/generalize: Similar overrides for DateData and TimeData?
}
