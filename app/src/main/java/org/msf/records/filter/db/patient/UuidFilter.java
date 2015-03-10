package org.msf.records.filter.db.patient;

import org.msf.records.data.app.AppPatient;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.sync.providers.Contracts;

/**
 * Returns only the patient with the given UUID.
 */
public final class UuidFilter extends SimpleSelectionFilter<AppPatient> {

    @Override
    public String getSelectionString() {
        return Contracts.Patients.UUID + "=?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { constraint.toString() };
    }
}
