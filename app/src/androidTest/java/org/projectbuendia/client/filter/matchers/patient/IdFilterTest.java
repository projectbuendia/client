// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.filter.matchers.patient;

import android.support.test.runner.AndroidJUnit4;

import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.projectbuendia.client.models.Patient;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

/** Tests for {@link IdFilter}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class IdFilterTest {
    private IdFilter mIdFilter;

    @Before
    public void createIdFilter() throws Exception {
        mIdFilter = new IdFilter();
    }

    /** Tests that id matching works in the base case. */
    @Test
    public void testMatches_exactMatch() {
        assertTrue(mIdFilter.matches(getPatientWithId("123"), "123"));
    }

    private Patient getPatientWithId(String id) {
        return new Patient(null, id, "", "", null, null, "");
    }

    /** Tests that id matching allows for a prefix match. */
    @Test
    public void testMatches_matchesPrefix() {
        assertTrue(mIdFilter.matches(getPatientWithId("123"), "12"));
    }

    /** Tests that id matching allows for a suffix match. */
    @Test
    public void testMatches_matchesSuffix() {
        assertTrue(mIdFilter.matches(getPatientWithId("123"), "23"));
    }

    /** Tests that id matching allows for an internal substring match. */
    @Test
    public void testMatches_matchesSubstring() {
        assertTrue(mIdFilter.matches(getPatientWithId("123"), "2"));
    }

    /** Tests that id matching supports unicode. */
    @Test
    public void testMatches_allowsUnicode() {
        assertTrue(mIdFilter.matches(getPatientWithId("ஐம்பது"), "ஐம்பது"));
    }

    /** Tests that id matching does not return false positives. */
    @Test
    public void testMatches_negativeMatch() {
        assertFalse(mIdFilter.matches(getPatientWithId("123"), "4"));
    }

    /** Tests that id matching is case-insensitive. */
    @Test
    public void testMatches_ignoresCase() {
        assertTrue(mIdFilter.matches(getPatientWithId("abc"), "ABC"));
    }
}
