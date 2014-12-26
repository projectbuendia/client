package org.msf.records.filter;

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
}
