package org.msf.records.filter;

/**
 * AllFilter is a pass-through filter that returns all results.
 */
public class AllFilter implements SimpleSelectionFilter {
    @Override
    public String getSelectionString() {
        return "";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[0];
    }
}
