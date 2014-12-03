package org.msf.records.filter;

/**
 * PregnantFilter returns only patients who are pregnant.
 */
public class PregnantFilter implements SimpleSelectionFilter {
    // TODO(akalachman): Implement.
    @Override
    public String getSelectionString() {
        return "";
    }

    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        return new String[0];
    }
}
