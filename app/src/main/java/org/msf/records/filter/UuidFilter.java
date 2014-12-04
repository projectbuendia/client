package org.msf.records.filter;

import org.msf.records.sync.PatientProviderContract;

/**
 * Created by shanee on 12/3/14.
 */
public class UuidFilter implements SimpleSelectionFilter {

    @Override
    public String getSelectionString() {
        return PatientProviderContract.PatientColumns.COLUMN_NAME_UUID + "=?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { constraint.toString() };
    }
}
