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

package org.projectbuendia.client.ui.matchers;

import android.support.test.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.PatientDelta;

/** Matchers for {@link Patient} objects. */
public class AppPatientMatchers {
    /**
     * Provides a {@link Matcher} that matches any patient with the given patient id.
     * @param id the id to match
     */
    public static Matcher<Object> isPatientWithId(final String id) {
        return new BoundedMatcher<Object, Patient>(Patient.class) {
            @Override public void describeTo(Description description) {
                description.appendText("is an Patient with id " + id);
            }

            @Override public boolean matchesSafely(Patient patient) {
                return id.equals(patient.id);
            }
        };
    }

    /**
     * Matches {@link PatientDelta} objects based on their JSON representations. It is assumed
     * that these JSON representations are stable and include all relevant fields, so two
     * {@link PatientDelta} objects with the same JSON representation should represent the
     * same patient.
     */
    public static Matcher<PatientDelta> matchesPatientDelta(final PatientDelta other) {
        return new BoundedMatcher<PatientDelta, PatientDelta>(PatientDelta.class) {
            @Override public void describeTo(Description description) {
                description.appendText("matches patientDelta: " + other.toString());
            }

            @Override public boolean matchesSafely(PatientDelta patientDelta) {
                return other.toString().equals(patientDelta.toString());
            }
        };
    }
}
