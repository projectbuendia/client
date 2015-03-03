package org.msf.records.filter.db;

import org.msf.records.data.app.AppTypeBase;

/**
 * A pass-through filter that returns all results.
 */
public final class AllFilter<T extends AppTypeBase> extends SimpleSelectionFilter<T> {
    @Override
    public String getSelectionString() {
        return "";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[0];
    }
}
