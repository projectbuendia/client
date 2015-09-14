// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.filter.matchers.patient;

import android.support.annotation.Nullable;

import org.projectbuendia.client.filter.matchers.MatchingFilter;
import org.projectbuendia.client.models.Patient;

import java.util.regex.Pattern;

/**
 * Filters by name.
 * <p/>
 * <p>Selects patients for whom each of the words in the parameter string prefix-match any of the
 * words in the given name or family name, even if in a different order.
 */
public final class NameFilter implements MatchingFilter<Patient> {
    private static final Pattern DASH_REGEX = Pattern.compile("^\\p{Pd}*$");

    @Override
    public boolean matches(@Nullable Patient patient, CharSequence constraint) {
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
        // for it in any word of the name.
        for (String searchTerm : searchTerms) {
            boolean termMatched = false;
            for (String namePart : nameParts) {
                // If both the search term and a name are dashes, use a more permissive matcher
                // that allows for an arbitrary type of dash. This makes it simpler to search for
                // patients with an unknown name, which is represented by a dash.
                if (namePart.startsWith(searchTerm) || areBothDashes(namePart, searchTerm)) {
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

    private boolean areBothDashes(String string1, String string2) {
        return isDash(string1) && isDash(string2);
    }

    private boolean isDash(String str) {
        return DASH_REGEX.matcher(str).matches();
    }
}
