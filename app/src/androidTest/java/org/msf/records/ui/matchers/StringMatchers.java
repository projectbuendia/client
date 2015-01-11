package org.msf.records.ui.matchers;

import com.google.android.apps.common.testing.ui.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.msf.records.data.app.AppPatient;

import java.util.regex.Pattern;

public class StringMatchers {
    public static Matcher<String> matchesRegex(final String regex) {
        return new TypeSafeMatcher<String>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("matches regex: " + regex);
            }

            @Override
            public boolean matchesSafely(String specimen) {
                return Pattern.matches(regex, specimen);
            }
        };
    }
}