package org.msf.records.filter.db;

import org.msf.records.sync.providers.Contracts;

/**
 * Returns only the patient with the given UUID.
 */
public final class UuidFilter implements SimpleSelectionFilter {

    @Override
    public String getSelectionString() {
        return Contracts.Patients.UUID + "=?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { constraint.toString() };
    }
}
