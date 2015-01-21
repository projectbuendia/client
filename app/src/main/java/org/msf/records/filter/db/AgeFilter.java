package org.msf.records.filter.db;

import org.joda.time.LocalDate;
import org.msf.records.sync.providers.Contracts;

/**
 * Returns only patients below a specified age in years, i.e.
 * whose birth dates were later than the specified number of years ago.
 */
final class AgeFilter implements SimpleSelectionFilter {
    private final int mYears;

    public AgeFilter(int years) {
        mYears = years;
    }

    @Override
    public String getSelectionString() {
        return Contracts.Patients.BIRTHDATE + " > ?";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        LocalDate earliestBirthdate = LocalDate.now().minusYears(mYears);
        return new String[] { earliestBirthdate.toString() };
    }
}