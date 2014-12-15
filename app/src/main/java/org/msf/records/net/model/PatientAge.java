package org.msf.records.net.model;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;

/**
 * Created by danieljulio on 13/10/2014.
 */
public class PatientAge {

    public int years;
    public int months;
    public String type;

    public Duration toDuration() {
        DateTime now = DateTime.now();

        if ("years".equals(type)) {
            return Duration.standardDays(years * 365);
        } else {
            return Duration.standardDays(months * 30);
        }
    }
}
