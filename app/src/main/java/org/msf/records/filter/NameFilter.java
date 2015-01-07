package org.msf.records.filter;

import org.msf.records.sync.providers.Contracts;

/**
 * A {@link SimpleSelectionFilter} that filters by name.
 *
 * <p>Selects patients for whom the parameter string prefix-matches any of the
 * words in the given name or family name.
 */
final class NameFilter implements SimpleSelectionFilter {

    @Override
    public String getSelectionString() {
        // To match the beginning of any word in the given or family name,
        // insert a space in front of each word and then look for a
        // space followed by the search key.
        return String.format("replace(' ' || %s || ' ' || %s, '-', ' ') LIKE ?",
                Contracts.Patients.GIVEN_NAME,
                Contracts.Patients.FAMILY_NAME);
    }

    /**
     * @param constraint the name prefix.
     */
    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        String wordPrefix = constraint.toString();
        return new String[] { "% " + wordPrefix + "%" };
    }
}
