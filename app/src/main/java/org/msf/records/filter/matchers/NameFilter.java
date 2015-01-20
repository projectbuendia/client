package org.msf.records.filter.matchers;

import android.support.annotation.Nullable;

import org.msf.records.data.app.AppPatient;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.sync.providers.Contracts;

/**
 * Filters by name.
 *
 * <p>Selects patients for whom the parameter string prefix-matches any of the
 * words in the given name or family name.
 */
public final class NameFilter implements MatchingFilter<AppPatient> {
    @Override
    public boolean matches(@Nullable AppPatient object, CharSequence constraint) {
        if (object == null) {
            return false;
        }
        String familyName = (object.familyName == null) ? "" : object.familyName.toLowerCase();
        String givenName = (object.givenName == null) ? "" : object.givenName.toLowerCase();
        return familyName.startsWith(constraint.toString().toLowerCase())
                || givenName.startsWith(constraint.toString().toLowerCase());
    }
}
