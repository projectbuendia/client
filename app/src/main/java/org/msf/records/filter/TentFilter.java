package org.msf.records.filter;

import org.msf.records.sync.PatientProviderContract;

/**
 * TentFilter selects only patients within the specified tent.
 */
public class TentFilter implements SimpleSelectionFilter {
    private String mTent;

    public TentFilter(String tent) {
        mTent = tent;
    }

    @Override
    public String getSelectionString() {
        return PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_TENT + "=?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { mTent };
    }
}
