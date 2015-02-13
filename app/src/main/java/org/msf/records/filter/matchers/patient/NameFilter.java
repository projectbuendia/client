package org.msf.records.filter.matchers.patient;

import android.support.annotation.Nullable;

import org.msf.records.data.app.AppPatient;
import org.msf.records.filter.matchers.MatchingFilter;

/**
 * Filters by name.
 *
 * <p>Selects patients for whom each of the words in the parameter string prefix-match any of the
 * words in the given name or family name, even if in a different order.
 */
public final class NameFilter implements MatchingFilter<AppPatient> {
    @Override
    public boolean matches(@Nullable AppPatient patient, CharSequence constraint) {
        if (patient == null) {
            return false;
        }

        // Get array of words that appear in any part of the name
        String givenName = (patient.givenName == null) ? "" : patient.givenName;
        String familyName = (patient.familyName == null) ? "" : patient.familyName;
        String fullName = givenName + " " + familyName;
        String[] nameParts = fullName.toLowerCase().split(" ");

        // Get array of words in the search query
        String[] searchTerms = constraint.toString().toLowerCase().split(" ");

        // Loop through each of the search terms checking if there is a prefix match
        // for it in any word of the name
        for (String searchTerm : searchTerms) {
            boolean termMatched = false;
            for (String namePart : nameParts) {
                if (namePart.startsWith(searchTerm)) {
                    termMatched = true;
                    break;  // no need to keep checking for this term
                }
            }
            // This search term was not matched to any word of the name,
            // so this patient is not a match
            if (!termMatched) {
                return false;
            }
        }

        // If we've been through all the search terms without returning false,
        // then we must have found a match for all of them
        return true;
    }
}
