package org.msf.records.filter;

import org.msf.records.sync.PatientProviderContract;

/**
 * AgeFilter returns only patients below a specified age in years (exact matches are NOT returned).
 */
public class AgeFilter implements SimpleSelectionFilter {
    private int mYears;

    public AgeFilter(int years) {
        mYears = years;
    }

    @Override
    public String getSelectionString() {
        return PatientProviderContract.PatientColumns.COLUMN_NAME_AGE_YEARS + " < ?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[] { Integer.toString(mYears) };
    }
}
