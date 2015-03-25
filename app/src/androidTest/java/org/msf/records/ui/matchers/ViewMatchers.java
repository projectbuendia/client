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

package org.msf.records.ui.matchers;

import android.graphics.drawable.Drawable;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;

/**
 * Matchers for {@link View} objects.
 */
public class ViewMatchers {

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