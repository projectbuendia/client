package org.msf.records.filter.db.patient;

import org.msf.records.data.app.AppPatient;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.model.Zone;
import org.msf.records.sync.providers.Contracts;

/**
 * Returns patients that are present (not discharged). This filter does NOT account for any
 * child locations of the Discharged zone, if any such location exists. This constraint allows
 * this filter to function even when locations have not been loaded.
 */
public class PresentFilter implements SimpleSelectionFilter<AppPatient> {
    private static final String SELECTION_STRING = Contracts.Patients.LOCATION_UUID + "!=?";
    private static final String[] SELECTION_ARGS = new String[] { Zone.DISCHARGED_ZONE_UUID };

    @Override
    public String getSelectionString() {
        return SELECTION_STRING;
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return SELECTION_ARGS;
    }

    @Override
    public String toString() {
        return "All Present Patients";
    }
}
