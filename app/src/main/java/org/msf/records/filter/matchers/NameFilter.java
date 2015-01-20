package org.msf.records.filter.matchers;

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
    public boolean matches(AppPatient object, CharSequence constraint) {
        return object.familyName.startsWith(constraint.toString())
                || object.givenName.startsWith(constraint.toString());
    }
}
