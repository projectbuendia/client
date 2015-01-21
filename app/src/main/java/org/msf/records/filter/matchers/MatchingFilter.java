package org.msf.records.filter.matchers;

/**
 * A filter which operates by selectively matching Objects of the given type.
 */
public interface MatchingFilter<T> {
    /**
     * Returns true iff the object matches this filter based on the given search term.
     *
     * @param object the object to match
     * @param constraint the search term
     */
    public boolean matches(T object, CharSequence constraint);
}
