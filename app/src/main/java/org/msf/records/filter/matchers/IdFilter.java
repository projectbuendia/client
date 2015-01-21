package org.msf.records.filter.matchers;

import android.support.annotation.Nullable;

import org.msf.records.data.app.AppPatient;

/**
 * Filter that matches based on user-specified id.
 */
public final class IdFilter implements MatchingFilter<AppPatient> {
    @Override
    public boolean matches(@Nullable AppPatient object, CharSequence constraint) {
        if (object == null || object.id == null) {
            return false;
        }
        return object.id.toLowerCase().contains(constraint.toString().toLowerCase());
    }
}
