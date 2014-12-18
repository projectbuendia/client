package org.msf.records.filter;

import org.joda.time.LocalDate;
import org.msf.records.sync.PatientProviderContract;

/**
 * AgeFilter returns only patients below a specified age in years, i.e.
 * whose birthdates were later than the specified number of years ago.
 */
public class AgeFilter implements SimpleSelectionFilter {
    private final int mYears;

    public AgeFilter(int years) {
        mYears = years;
    }

    @Override
    public String getSelectionString() {
        return PatientProviderContract.PatientColumns.COLUMN_NAME_BIRTHDATE + " > ?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        LocalDate earliestBirthdate = LocalDate.now().minusYears(mYears);
        return new String[] { earliestBirthdate.toString() };
    }
}