package org.msf.records.filter.db;

import org.msf.records.data.app.AppTypeBase;
import org.msf.records.filter.db.SimpleSelectionFilter;

/**
 * A pass-through filter that returns all results.
 */
public final class AllFilter<T extends AppTypeBase> implements SimpleSelectionFilter<T> {
    @Override
    public String getSelectionString() {
        return "";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[0];
    }
}
