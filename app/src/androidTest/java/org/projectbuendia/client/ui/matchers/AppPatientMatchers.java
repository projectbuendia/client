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

import com.google.android.apps.common.testing.ui.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.AppPatientDelta;

/** Matchers for {@link AppPatient} objects. */
public class AppPatientMatchers {
    /**
     * Provides a {@link Matcher} that matches any patient with the given patient id.
     * @param id the id to match
     */
    public static Matcher<Object> isPatientWithId(final String id) {
        return new BoundedMatcher<Object, AppPatient>(AppPatient.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("is an AppPatient with id " + id);
            }

            @Override
            public boolean matchesSafely(AppPatient patient) {
                return id.equals(patient.id);
            }
        };
    }

    /**
     * Matches {@link AppPatientDelta} objects based on their JSON representations. It is assumed
     * that these JSON representations are stable and include all relevant fields, so two
     * {@link AppPatientDelta} objects with the same JSON representation should represent the
     * same patient.
     */
    public static Matcher<AppPatientDelta> matchesPatientDelta(final AppPatientDelta other) {
        return new BoundedMatcher<AppPatientDelta, AppPatientDelta>(AppPatientDelta.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("matches appPatientDelta: " + other.toString());
            }

            @Override
            public boolean matchesSafely(AppPatientDelta patientDelta) {
                return other.toString().equals(patientDelta.toString());
            }
        };
    }
}
