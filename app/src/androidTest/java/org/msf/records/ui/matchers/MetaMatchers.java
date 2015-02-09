package org.msf.records.ui.matchers;

import android.view.View;

import com.google.android.apps.common.testing.ui.espresso.ViewAssertion;

import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;
import org.msf.records.utils.Logger;

import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;

public class MetaMatchers {
    private static final Logger LOG = Logger.create();

    /**
     * Periodically applies the matcher, asserting that the matcher is matched at some point before
     * the specified timeout is reached. This may be useful in cases where views are being updated
     * quickly (but asynchronously) and no event bus event is available for use with
     * {@link org.msf.records.ui.sync.EventBusIdlingResource}.
     */
    public static ViewAssertion matchesWithin(final Matcher<View> matcher, final long timeoutMs) {
        return matches(within(matcher, timeoutMs));
    }

    private static ArgumentMatcher<View> within(
            final Matcher<View> matcher, final long timeoutMs) {
        return new ArgumentMatcher<View>() {
            final int CHECK_INTERVAL_MILLIS = 200;

            @Override
            public boolean matches(final Object o) {
                View v = (View)o;
                LOG.v("Before matching loop");
                long timeoutTime = System.currentTimeMillis() + timeoutMs;
                while (timeoutTime > System.currentTimeMillis()) {
                    LOG.v("In matching loop");
                    long endTime = System.currentTimeMillis() + CHECK_INTERVAL_MILLIS;
                    // Check internal matcher.
                    if (matcher.matches(v)) {
                        return true;
                    }
                    // Wait until the next check interval.
                    while (System.currentTimeMillis() < endTime) ;
                }

                // Timeout.
                LOG.v("returning false");
                return false;
            }
        };
    }
}