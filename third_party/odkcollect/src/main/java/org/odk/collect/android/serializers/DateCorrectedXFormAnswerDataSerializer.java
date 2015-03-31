package org.odk.collect.android.serializers;

import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.xform.util.XFormAnswerDataSerializer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * An {@link XFormAnswerDataSerializer} that correctly formats dates in the ISO8601 format. The
 * default XFormAnswerDataSerializer has logic to perform this task, but fails in some corner cases.
 */
public class DateCorrectedXFormAnswerDataSerializer extends XFormAnswerDataSerializer {
    private static final DateFormat ISO8601_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    @Override
    public Object serializeAnswerData(DateTimeData data) {
        ISO8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        return ISO8601_FORMAT.format((Date)data.getValue());
    }

    // TODO/generalize: Similar overrides for DateData and TimeData?
}
