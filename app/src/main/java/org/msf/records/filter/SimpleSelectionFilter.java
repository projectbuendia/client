package org.msf.records.filter;

/**
 * SimpleSelectionFilter is a container for a filter string (part of an SQL WHERE
 * clause) and the arguments to insert into that filter string.
 */
public interface SimpleSelectionFilter {
    /**
     * A selection filter, with the syntax of a structured SQL WHERE clause.
     * For example, a selection filter could be "given_name=? AND family_name LIKE ?".
     */
    public String getSelectionString();

    /**
     * Selection arguments that map to any wild cards in the selection string.
     * For example, for the selection filter "given_name=? and family_name LIKE ?",
     * getSelectionArg(CharSequence) should return two strings.
     * @param constraint the constraint passed into the top-level filter
     */
    public String[] getSelectionArgs(CharSequence constraint);
}