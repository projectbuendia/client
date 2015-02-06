package org.msf.records.filter.matchers;

import android.support.annotation.Nullable;

import org.msf.records.data.app.AppPatient;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.sync.providers.Contracts;

/**
 * Filters by name.
 *
 * <p>Selects patients for whom each of the words in the parameter string prefix-match any of the
 * words in the given name or family name, even if in a different order.
 */
public final class NameFilter implements MatchingFilter<AppPatient> {
    @Override
    public boolean matches(@Nullable AppPatient object, CharSequence constraint) {
        if (object == null) {
            return false;
        }
        String familyName = (object.familyName == null) ? "" : object.familyName.toLowerCase();
        String givenName = (object.givenName == null) ? "" : object.givenName.toLowerCase();

        // Get array of words that appear in any part of the name
        String fullName = givenName + " " + familyName;
        String[] nameParts = fullName.split(" ");

        // Get array of words in the search query
        String[] searchTerms = constraint.toString().toLowerCase().split(" ");

        // Loop through each of the search terms checking if there is a prefix match for each in any word of the name
        boolean found = false;
        for(int i = 0; i < searchTerms.length; i++) {
            found = false;
            for(int j = 0; j < nameParts.length; j++) {
                if nameParts[j].startsWith(searchTerms[i]) {}
                    found = true;
                    break;
                }
            }
            // This search term was not found in any word of the name, so this patient is not a match
            if !found return false;
        }

        // If we've been through all the search terms without returning false, then we must have found a match for all of them
        return true;
    }
}
