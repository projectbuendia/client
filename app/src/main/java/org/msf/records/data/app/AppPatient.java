package org.msf.records.data.app;

import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * An app model patient.
 */
public class AppPatient extends AppTypeBase<String> {

    public static final int GENDER_UNKNOWN = 0;
    public static final int GENDER_MALE = 1;
    public static final int GENDER_FEMALE = 2;

    public String uuid;

    public String givenName;
    public String familyName;
    public Duration age;
    public int gender;

    public DateTime admissionDateTime;

    public String locationUuid;
}
