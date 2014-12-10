package org.msf.records.data.app;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

/**
 * An app model patient.
 */
public class AppPatient extends AppTypeBase<String> {

    public static final int GENDER_UNKNOWN = 0;
    public static final int GENDER_MALE = 1;
    public static final int GENDER_FEMALE = 2;

    public String mUuid;

    public String mGivenName;
    public String mFamilyName;
    public Duration mAge;
    public int mGender;

    public DateTime mAdmissionDateTime;

    public String mLocationUuid;
}
