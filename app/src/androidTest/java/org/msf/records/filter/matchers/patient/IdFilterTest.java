package org.msf.records.filter.matchers.patient;

import android.test.InstrumentationTestCase;

import org.msf.records.data.app.AppPatient;

/**
 * Tests for {@link IdFilter}.
 */
public class IdFilterTest extends InstrumentationTestCase {
    private IdFilter mIdFilter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mIdFilter = new IdFilter();
    }

    /** Tests that id matching works in the base case. */
    public void testMatches_exactMatch() {
        assertTrue(mIdFilter.matches(getPatientWithId("123"), "123"));
    }

    /** Tests that id matching allows for a prefix match. */
    public void testMatches_matchesPrefix() {
        assertTrue(mIdFilter.matches(getPatientWithId("123"), "12"));
    }

    /** Tests that id matching allows for a suffix match. */
    public void testMatches_matchesSuffix() {
        assertTrue(mIdFilter.matches(getPatientWithId("123"), "23"));
    }

    /** Tests that id matching allows for an internal substring match. */
    public void testMatches_matchesSubstring() {
        assertTrue(mIdFilter.matches(getPatientWithId("123"), "2"));
    }

    /** Tests that id matching supports unicode. */
    public void testMatches_allowsUnicode() {
        assertTrue(mIdFilter.matches(getPatientWithId(""), ""));
    }

    /** Tests that id matching does not return false positives. */
    public void testMatches_negativeMatch() {
        assertFalse(mIdFilter.matches(getPatientWithId("123"), "4"));
    }

    /** Tests that id matching is case-insensitive. */
    public void testMatches_ignoresCase() {
        assertTrue(mIdFilter.matches(getPatientWithId("abc"), "ABC"));
    }

    private AppPatient getPatientWithId(String id) {
        return AppPatient.builder().setId(id).build();
    }
}
