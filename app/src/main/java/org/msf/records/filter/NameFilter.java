package org.msf.records.filter;

import org.msf.records.sync.PatientProviderContract;

/**
 * NameFilter is a SimpleSelectionFilter that filters by name.
 */
public class NameFilter implements SimpleSelectionFilter {
    /**
     * Selects patients for whom the parameter string prefix-matches any of the
     * words in the given name or family name.
     * @return String
     */
    @Override
    public String getSelectionString() {
        // To match the beginning of any word in the given or family name,
        // insert a space in front of each word and then look for a
        // space followed by the search key.
        return String.format("replace(' ' || %s || ' ' || %s, '-', ' ') LIKE ?",
                PatientProviderContract.PatientColumns.COLUMN_NAME_GIVEN_NAME,
                PatientProviderContract.PatientColumns.COLUMN_NAME_FAMILY_NAME);
    }

    /**
     * Selects any patient whose given name or family name contains a word starting
     * with the given prefix string.
     * @param constraint The name prefix.
     * @return
     */
    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        String wordPrefix = constraint.toString();
        return new String[] { "% " + wordPrefix + "%" };
    }
}
