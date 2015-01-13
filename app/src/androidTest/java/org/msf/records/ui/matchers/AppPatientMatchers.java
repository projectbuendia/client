package org.msf.records.ui.matchers;

import com.google.android.apps.common.testing.ui.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.msf.records.data.app.AppPatient;

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
}