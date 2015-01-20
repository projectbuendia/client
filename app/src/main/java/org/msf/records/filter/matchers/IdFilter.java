package org.msf.records.filter.matchers;

import org.msf.records.data.app.AppPatient;

/**
 * Filter that matches based on user-specified id.
 */
public final class IdFilter implements MatchingFilter<AppPatient> {
    @Override
    public boolean matches(AppPatient object, CharSequence constraint) {
        return object.id.contains(constraint);
    }
}
