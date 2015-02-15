package org.msf.records.ui.matchers;

import android.graphics.drawable.Drawable;
import android.view.View;

import com.google.android.apps.common.testing.ui.espresso.ViewAssertion;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;
import org.msf.records.utils.Logger;

import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;

public class ViewMatchers {
    private static final Logger LOG = Logger.create();
    private static final int CHECK_INTERVAL_MILLIS = 200;

    /**
     * Periodically applies the matcher, asserting that the matcher is matched at some point before
     * the specified timeout is reached. This may be useful in cases where views are being updated
     * quickly (but asynchronously) and no event bus event is available for use with
     * {@link org.msf.records.ui.sync.EventBusIdlingResource}.
     */
    public static ViewAssertion matchesWithin(final Matcher<View> matcher, final long timeoutMs) {
        return matches(within(matcher, timeoutMs));
    }

    /**
     * Provides a {@link Matcher} that wraps another {@link Matcher}, periodically applying it and
     * returning true if the {@link View} is matched within a specified period of time.
     */
    private static ArgumentMatcher<View> within(
            final Matcher<View> matcher, final long timeoutMs) {
        return new ArgumentMatcher<View>() {

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
                    if (endTime > System.currentTimeMillis()) {
                        try {
                            Thread.sleep(endTime - System.currentTimeMillis());
                        } catch (InterruptedException e) {
                            LOG.w("Unable to sleep");
                        }
                    }
                }

                // Timeout.
                LOG.v("returning false");
                return false;
            }
        };
    }

    /**
     * Provides a {@link Matcher} that matches any view in the given row, where each row has a
     * given height.
     */
    public static ArgumentMatcher<View> inRow(final int rowNumber, final int rowHeight) {
        return new ArgumentMatcher<View>() {
            private int getMinY() {
                return rowNumber * rowHeight;
            }

            private int getMaxY() {
                return (rowNumber + 1) * rowHeight;
            }

            @Override
            public boolean matches(final Object o) {
                View v = (View)o;
                return v.getY() >= getMinY() && v.getY() < getMaxY();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has y value between " + getMinY() + " and " + getMaxY());
            }
        };
    }

    /**
     * Provides a {@link Matcher} that matches any view in the given column, where each column has
     * the given width.
     */
    public static ArgumentMatcher<View> inColumn(final int colNumber, final int colWidth) {
        return new ArgumentMatcher<View>() {
            private int getMinX() {
                return colNumber * colWidth;
            }

            private int getMaxX() {
                return (colNumber + 1) * colWidth;
            }

            @Override
            public boolean matches(final Object o) {
                View v = (View)o;
                return v.getX() >= getMinX() && v.getX() < getMaxX();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has x value between " + getMinX() + " and " + getMaxX());
            }
        };
    }

    /**
     * Provides a {@link Matcher} that matches any view with the given background drawable.
     */
    public static ArgumentMatcher<View> hasBackground(final Drawable background) {
        return new ArgumentMatcher<View>() {

            @Override
            public boolean matches(final Object o) {
                View v = (View)o;

                if (background == null || v.getBackground() == null) {
                    return false;
                }

                return background.getConstantState().equals(v.getBackground().getConstantState());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has background: " + background.toString());
            }
        };
    }
}