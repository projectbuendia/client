package org.msf.records.ui.matchers;

import com.google.android.apps.common.testing.ui.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;

public class AppPatientMatchers {
    public static Matcher<Object> isPatientWithId(final Matcher<String> id) {
        return new BoundedMatcher<Object, AppPatient>(AppPatient.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("is an AppPatient with id: ");
                id.describeTo(description);
            }

            @Override
            public boolean matchesSafely(AppPatient patient) {
                return id.matches(patient.id);
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