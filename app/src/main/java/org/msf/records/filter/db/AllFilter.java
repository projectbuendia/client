package org.msf.records.filter.db;

/**
 * A pass-through filter that returns all results.
 */
public final class AllFilter implements SimpleSelectionFilter {
    @Override
    public String getSelectionString() {
        return "";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[0];
    }

    @Override
    public String toString() { return "All Patients"; }
}
