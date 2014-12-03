package org.msf.records.filter;

import org.msf.records.sync.PatientProviderContract;

/**
 * IdFilter is a SimpleSelectionFilter that filters by user-specified id.
 */
public class IdFilter implements SimpleSelectionFilter {
    @Override
    public String getSelectionString() {
        return PatientProviderContract.PatientColumns._ID + "=?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { constraint.toString() };
    }
}
