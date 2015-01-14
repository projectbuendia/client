package org.msf.records.net.model;

import org.joda.time.DateTime;

/**
 * Simple wrapper json bean to help parsing results for multiple patients.
 */
public class PatientChartList {
    public PatientChart [] results;
    public DateTime snapshotTime;
}
