package org.msf.records.filter;

import org.msf.records.sync.providers.Contracts;

/**
 * IdFilter is a SimpleSelectionFilter that filters by user-specified id.
 */
final class IdFilter implements SimpleSelectionFilter {
    @Override
    public String getSelectionString() {
        return Contracts.Patients._ID + " LIKE ?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { "%" + constraint.toString() + "%" };
    }
}
