package org.msf.records.filter;

import org.msf.records.sync.PatientProviderContract;

/**
 * NameFilter is a SimpleSelectionFilter that filters by name.
 */
public class NameFilter implements SimpleSelectionFilter {
    /**
     * Selects patients with a matching family or given name.
     * @return
     */
    @Override
    public String getSelectionString() {
        return PatientProviderContract.PatientColumns.COLUMN_NAME_FAMILY_NAME + " LIKE ? OR " +
               PatientProviderContract.PatientColumns.COLUMN_NAME_GIVEN_NAME + " LIKE ?";
    }

    /**
     * Selects any name that starts with the given prefix string.
     * @param constraint the name prefix
     * @return
     */
    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        String nameArg = constraint.toString() + "%";
        return new String[] { nameArg, nameArg };
    }
}
