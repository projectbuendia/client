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

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.espresso.web.assertion.WebAssertion;
import android.support.test.espresso.web.model.Atom;
import android.support.test.espresso.web.model.ElementReference;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Checkable;

import com.estimote.sdk.internal.Preconditions;
import com.google.common.base.Joiner;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;
import org.projectbuendia.client.utils.Utils;

import java.text.MessageFormat;

import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.allOf;
import static org.projectbuendia.client.utils.Utils.eq;

/** Matchers for {@link View}s. */
public class TestCaseWithMatcherMethods<T extends Activity> extends ActivityTestRule<T> {

    public TestCaseWithMatcherMethods(Class<T> startingActivity) {
        super(startingActivity, true, true);
    }

    // More concise ways of expressing common constructions like
    // onView(hasId(...)), onView(...).check(matches(...)), onView(...).perform(...).

    public static ViewInteraction viewWithId(int id) {
        return Espresso.onView(hasId(id));
    }

    public static Matcher<View> hasId(int id) {
        return new MatcherWithDescription<>(ViewMatchers.withId(id), "has ID " + id);
    }

    public static ViewInteraction viewWithText(Object obj) {
        return Espresso.onView(hasText(obj));
    }

    public static ViewInteraction viewContainingText(String text) {
        return Espresso.onView(hasTextContaining(text));
    }

    public static ViewInteraction firstViewWithText(Object obj) {
        return firstViewThat(hasText(obj));
    }

    public static ViewInteraction firstViewContainingText(String text) {
        return firstViewThat(hasTextContaining(text));
    }

    public static Matcher<View> hasText(Object obj) {
        String text = "" + obj;
        return new MatcherWithDescription<>(ViewMatchers.withText(text),
            "has the exact text \"" + quote(text) + "\"");
    }

    public static ViewInteraction viewWithTextString(int resourceId) {
        return Espresso.onView(hasTextString(resourceId));
    }

    public static Matcher<View> hasTextString(int resourceId) {
        return new MatcherWithDescription<>(ViewMatchers.withText(resourceId),
            "has string resource " + resourceId + " as its text");
    }

    public static ViewInteraction firstViewThat(Matcher<View>... matchers) {
        final Matcher matcher = combinedMatcher(matchers);
        return Espresso.onView(new TypeSafeMatcher<View>() {
            int currentIndex = 0;
            View foundView = null;

            @Override
            public void describeTo(Description description) {
                description.appendText("is the first match that ");
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (view == foundView || matcher.matches(view)) {
                    if (foundView == null) foundView = view;
                }
                return (view == foundView);
            }
        });
    }

    @SafeVarargs public static ViewInteraction viewThat(Matcher<View>... matchers) {
        return Espresso.onView(combinedMatcher(matchers));
    }

    public View getViewThat(Matcher<View>... matchers) {
        final View[] holder = {null};
        Matcher capturer = new TypeSafeMatcher<View>() {
            @Override protected boolean matchesSafely(View item) {
                holder[0] = item;
                return true;
            }
            @Override public void describeTo(Description description) { }
        };
        waitUntilVisible(viewThat(combinedMatcher(matchers), capturer));
        return holder[0];
    }

    public static DataInteraction dataThat(Matcher... matchers) {
        return Espresso.onData(matchers.length > 1 ? allOf(matchers) : matchers[0]);
    }

    @SafeVarargs public static void expect(ViewInteraction vi, Matcher<View>... matchers) {
        vi.check(matches(combinedMatcher(matchers)));
    }

    @SafeVarargs public static void expect(DataInteraction di, Matcher<View>... matchers) {
        di.check(matches(combinedMatcher(matchers)));
    }

    public static Matcher<View> combinedMatcher(Matcher<View>... matchers) {
        return matchers.length == 0 ? exists() :
            matchers.length > 1 ? allOf(matchers) : matchers[0];
    }

    public static Matcher<View> exists() {
        return new TypeSafeMatcher<View>() {
            @Override public void describeTo(Description description) {
                description.appendText("exists");
            }

            @Override public boolean matchesSafely(View view) {
                return view != null;
            }
        };
    }

    public static void expectVisible(ViewInteraction vi) {
        expect(vi, isVisible());
    }

    public static void expectVisible(DataInteraction di) {
        expect(di, isVisible());
    }

    public static Matcher<View> isVisible() {
        return new MatcherWithDescription<>(ViewMatchers.isDisplayed(), "is visible");
    }

    public static void click(DataInteraction di) {
        di.perform(ViewActions.click());
    }

    public static void openActionBarOptionsMenu() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
    }

    public static void optionallyScrollTo(ViewInteraction vi) {
        try {
            vi.perform(ViewActions.scrollTo());
        } catch (RuntimeException e) {
            /* if scrolling isn't possible, proceed anyway */
        }
    }

    public static void click(ViewInteraction vi) {
        optionallyScrollTo(vi);
        vi.perform(ViewActions.click());
    }

    public static void clickIfUnchecked(ViewInteraction vi) {
        optionallyScrollTo(vi);
        vi.perform(clickIfUncheckedAction());
    }

    public static void act(ViewInteraction vi, ViewAction action) {
        vi.perform(action);
    }

    public static void type(Object obj, ViewInteraction vi) {
        waitFor(vi);
        optionallyScrollTo(vi);
        vi.perform(ViewActions.typeText(obj.toString()));
    }

    public static void clearAndType(Object obj, ViewInteraction vi) {
        waitFor(vi);
        vi.perform(ViewActions.clearText()).perform(ViewActions.typeText(obj.toString()));
    }

    public static void expect(int id) {
        expect(viewWithId(id));
    }

    public static void expect(String text) {
        expect(firstViewWithText(text));
    }

    public static void click(int id) {
        click(viewWithId(id));
    }

    public static void click(String text) {
        click(firstViewWithText(text));
    }

    public static void type(Object obj, int id) {
        type(obj, viewWithId(id));
    }

    public static void clearAndType(Object obj, int id) {
        clearAndType(obj, viewWithId(id));
    }

    public static void act(int id, ViewAction action) {
        act(viewWithId(id), action);
    }

    public static void act(String text, ViewAction action) {
        act(firstViewWithText(text), action);
    }

    public static void expect(Atom<ElementReference> atom) {
        onWebView().check(elementExists(atom));
    }

    public static WebAssertion<ElementReference> elementExists(Atom<ElementReference> atom) {
        return new WebAssertion<ElementReference>(atom) {
            @Override public void checkResult(WebView view, ElementReference result) {
                ViewMatchers.assertThat(result, new BaseMatcher<ElementReference>() {
                    @Override public void describeTo(Description description) {
                        description.appendText("exists");
                    }

                    @Override public boolean matches(Object item) {
                        return item != null;
                    }
                });
            }
        };
    }

    public static void click(Atom<ElementReference> atom) {
        onWebView().withElement(atom).perform(webClick());
    }

    // Matchers with better descriptions than those in espresso.matcher.ViewMatchers.

    public static void scrollToAndExpectVisible(ViewInteraction vi) {
        optionallyScrollTo(vi);
        vi.check(matches(isVisible()));
    }

    public static Matcher<View> isA(final Class<? extends View> cls) {
        String name = cls.getSimpleName();
        return new MatcherWithDescription<>(
            ViewMatchers.isAssignableFrom(cls),
            (name.matches("^[AEIOU]") ? "is an " : "is a ") + quote(name));
    }

    // Names of Espresso matchers form expressions that don't make any grammatical sense,
    // such as withParent(withSibling(isVisible())).  Instead of prepositional
    // phrases like "withFoo", matcher names should be verb phrases like "hasFoo" or
    // connecting verb phrases ending in "That", yielding more readable expressions
    // such as whoseParent(hasSiblingThat(isVisible())).

    public static Matcher<View> isAnyOf(final Class<? extends View>... classes) {
        Preconditions.checkArgument(classes.length >= 1);
        return new TypeSafeMatcher<View>() {
            @Override public boolean matchesSafely(View obj) {
                for (Class cls : classes) {
                    if (cls.isInstance(obj)) return true;
                }
                return false;
            }

            @Override public void describeTo(Description description) {
                String[] names = new String[classes.length];
                for (int i = 0; i < classes.length; i++) {
                    names[i] = classes[i].getSimpleName();
                }
                String list = Joiner.on(", ").join(names);
                if (names.length == 2) {
                    list = list.replace(", ", " or ");
                } else if (names.length > 2) {
                    list = list.replaceAll(", ([^,]*)$", ", or $1");
                }
                description.appendText(
                    (names[0].matches("^[AEIOU]") ? "is an " : "is a ") + list);
            }
        };
    }

    @SafeVarargs
    public static Matcher<View> whoseParent(Matcher<View>... matchers) {
        Matcher<View> matcher = matchers.length > 1 ? allOf(matchers) : matchers[0];
        return new MatcherWithDescription<>(ViewMatchers.withParent(matcher),
            "whose parent {0}", matcher);
    }

    @SafeVarargs
    public static Matcher<View> hasChildThat(Matcher<View>... matchers) {
        Matcher<View> matcher = matchers.length > 1 ? allOf(matchers) : matchers[0];
        return new MatcherWithDescription<>(ViewMatchers.withChild(matcher),
            "has a child that {0}", matcher);
    }

    @SafeVarargs
    public static Matcher<View> hasAncestorThat(Matcher<View>... matchers) {
        Matcher<View> matcher = matchers.length > 1 ? allOf(matchers) : matchers[0];
        return new MatcherWithDescription<>(ViewMatchers.isDescendantOfA(matcher),
            "has an ancestor that {0}", matcher);
    }

    @SafeVarargs
    public static Matcher<View> hasDescendantThat(Matcher<View>... matchers) {
        Matcher<View> matcher = matchers.length > 1 ? allOf(matchers) : matchers[0];
        return new MatcherWithDescription<>(ViewMatchers.hasDescendant(matcher),
            "has a descendant that {0}", matcher);
    }

    @SafeVarargs
    public static Matcher<View> hasSiblingThat(Matcher<View>... matchers) {
        Matcher<View> matcher = matchers.length > 1 ? allOf(matchers) : matchers[0];
        return new MatcherWithDescription<>(ViewMatchers.hasSibling(matcher),
            "has a sibling that {0}", matcher);
    }

    public static Matcher<View> isChecked() {
        return new MatcherWithDescription<>(ViewMatchers.isChecked(), "is checked");
    }

    public static Matcher<View> isNotChecked() {
        return new MatcherWithDescription<>(ViewMatchers.isNotChecked(), "is unchecked");
    }

    @SafeVarargs
    public static Matcher<View> hasText(Matcher<String>... matchers) {
        Matcher<String> matcher = matchers.length > 1 ? allOf(matchers) : matchers[0];
        return new MatcherWithDescription<>(ViewMatchers.withText(matcher),
            "has text {0}", matcher);
    }

    public static Matcher<View> hasTextContaining(String text) {
        return new MatcherWithDescription<>(ViewMatchers.withText(Matchers.containsString(text)),
            "has text containing \"" + quote(text) + "\"");
    }

    public static Matcher<View> hasTextMatchingRegex(String regex) {
        return new MatcherWithDescription<>(
            ViewMatchers.withText(StringMatchers.matchesRegex(regex)),
            "has text matching regex /" + quote(regex) + "/");
    }

    public static Matcher<View> isAtLeastNPercentVisible(int percentage) {
        return new MatcherWithDescription<>(ViewMatchers.isDisplayingAtLeast(percentage),
            "is at least " + percentage + "% visible");
    }

    /** Matcher that matches any view in the given row, assuming all rows have the specified height. */
    public static TypeSafeMatcher<View> isInRow(final int rowNumber, final int rowHeight) {
        return new TypeSafeMatcher<View>() {
            @Override public boolean matchesSafely(View view) {
                return view.getY() >= getMinY() && view.getY() < getMaxY();
            }

            private int getMaxY() {
                return (rowNumber + 1)*rowHeight;
            }

            private int getMinY() {
                return rowNumber*rowHeight;
            }

            @Override public void describeTo(Description description) {
                description.appendText("has " + getMinY() + " <= y < " + getMaxY());
            }
        };
    }

    /** Matcher that matches any view in the given column, assuming all columns have the specified with. */
    public static TypeSafeMatcher<View> isInColumn(final int colNumber, final int colWidth) {
        return new TypeSafeMatcher<View>() {
            @Override public boolean matchesSafely(View view) {
                return view.getX() >= getMinX() && view.getX() < getMaxX();
            }

            private int getMaxX() {
                return (colNumber + 1)*colWidth;
            }

            private int getMinX() {
                return colNumber*colWidth;
            }

            @Override public void describeTo(Description description) {
                description.appendText("has " + getMinX() + " <= x < " + getMaxX());
            }
        };
    }

    /** Matcher that matches any view with the given background drawable. */
    public static TypeSafeMatcher<View> hasBackground(final Drawable background) {
        return new TypeSafeMatcher<View>() {
            @Override public boolean matchesSafely(View view) {
                return background != null && view.getBackground() != null &&
                    background.getConstantState().equals(
                        view.getBackground().getConstantState());
            }

            @Override public void describeTo(Description description) {
                description.appendText("has background " + background.toString());
            }
        };
    }

    /** Matcher that matches a view with the background drawable specified by ID. */
    public static TypeSafeMatcher<View> hasBackground(final int resourceId) {
        return new TypeSafeMatcher<View>() {
            @Override public boolean matchesSafely(View view) {
                Drawable background = view.getResources().getDrawable(resourceId);
                return background != null && view.getBackground() != null &&
                    background.getConstantState().equals(
                        view.getBackground().getConstantState());
            }

            @Override public void describeTo(Description description) {
                description.appendText("has drawable resource " + resourceId + " as its background");
            }
        };
    }

    protected static ViewInteraction waitFor(int id) {
        return waitFor(viewWithId(id));
    }

    protected static ViewInteraction waitFor(String text) {
        return waitFor(firstViewWithText(text));
    }

    protected static ViewInteraction waitFor(ViewInteraction vi) {
        waitUntil(vi, exists());
        return vi;
    }

    protected static ViewInteraction waitUntil(ViewInteraction vi, Matcher<View>... matchers) {
        return waitUntil(30000, vi, matchers);
    }

    protected static ViewInteraction waitUntilVisible(ViewInteraction vi) {
        return waitUntilVisible(30000, vi);
    }

    protected static ViewInteraction waitUntilVisible(int timeoutMs, ViewInteraction vi) {
        return waitUntil(timeoutMs, vi, isVisible());
    }

    protected static ViewInteraction waitUntil(int timeoutMs, ViewInteraction vi, Matcher<View>... matchers) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            try {
                expect(vi, matchers);
                return vi;
            } catch (Throwable t) {
                if (System.currentTimeMillis() > deadline) throw t;
            }
            sleep(100);
        }
    }

    public static void back() {
        pressBack();
        sleep(500);
    }

    public static void sleep(int millis) {
        SystemClock.sleep(millis);
    }

    /** Replaces the description of an existing matcher. */
    static class MatcherWithDescription<T> extends TypeSafeMatcher<T> {
        Matcher mMatcher;
        String mFormat;
        Matcher[] mArgMatchers;

        public MatcherWithDescription(Matcher<T> matcher, String format, Matcher... argMatchers) {
            mMatcher = matcher;
            mFormat = format;
            mArgMatchers = argMatchers;
        }

        public boolean matchesSafely(T obj) {
            return mMatcher.matches(obj);
        }

        public void describeTo(Description description) {
            Object[] args = new String[mArgMatchers.length];
            for (int i = 0; i < mArgMatchers.length; i++) {
                StringDescription argDescription = new StringDescription();
                mArgMatchers[i].describeTo(argDescription);
                args[i] = argDescription.toString();
            }
            description.appendText(MessageFormat.format(mFormat, args));
        }
    }

    /** Quotes a string for use in a MessageFormat. */
    private static String quote(String text) {
        if (Utils.isEmpty(text)) return "";
        return "'" + text.replaceAll("'", "''") + "'";
    }

    static ViewAction clickIfUncheckedAction() {
        return new ViewAction() {
            @Override public BaseMatcher<View> getConstraints() {
                return new BaseMatcher<View>() {
                    @Override public boolean matches(Object item) {
                        return Matchers.isA(Checkable.class).matches(item); }

                    @Override public void describeMismatch(Object item, Description description) { }

                    @Override public void describeTo(Description description) { }
                };
            }

            @Override public String getDescription() {
                return null;
            }

            @Override public void perform(UiController uiController, View view) {
                Checkable checkableView = (Checkable) view;
                if (!checkableView.isChecked()) {
                    new GeneralClickAction(Tap.SINGLE, GeneralLocation.VISIBLE_CENTER, Press.FINGER)
                        .perform(uiController, view);
                }
            }
        };
    }

    /** This matcher is designed only to be used repeatedly in a waitFor() call. */
    protected Matcher<View> containsElementWithId(String id) {
        return new TypeSafeMatcher<View>() {
            private View matchedView = null;

            @Override protected boolean matchesSafely(View view) {
                if (view == matchedView) return true;
                if (view instanceof WebView) {
                    ((WebView) view).evaluateJavascript(
                        "document.getElementById('" + id + "')",
                        result -> {
                            if (!eq(result, "null")) {
                                matchedView = view;
                                Log.i("buendia/TestCase", "Found element '" + id + "'");
                            }
                        }
                    );
                }
                return false;
            }

            @Override public void describeTo(Description description) {
                description.appendText("is a WebView containing an element with ID \"" + id + "\"");
            }
        };
    }

    protected void clickElementWithId(String id) {
        waitFor(viewThat(containsElementWithId(id))).perform(new ViewAction() {
            @Override public String getDescription() {
                return "click element with id \"" + id + "\"";
            }

            @Override public Matcher<View> getConstraints() {
                return isA(WebView.class);
            }

            @Override public void perform(UiController controller, View view) {
                Log.i("buendia/TestCase", "Clicking element '" + id + "'");
                ((WebView) view).evaluateJavascript(
                    "var e = document.getElementById('" + id + "'); " +
                    "e.dispatchEvent(new Event('touchstart')); e.click()", null
                );
            }
        });
    }

    /** This matcher is designed only to be used repeatedly in a waitFor() call. */
    protected Matcher<View> containsElementWithIdAndText(String id, String text) {
        return new TypeSafeMatcher<View>() {
            private View matchedView = null;

            @Override protected boolean matchesSafely(View view) {
                if (view == matchedView) return true;
                if (view instanceof WebView) {
                    ((WebView) view).evaluateJavascript(
                        "document.getElementById('" + id + "').textContent",
                        result -> {
                            if (eq(result, '"' + text + '"')) {
                                matchedView = view;
                                Log.i("buendia/TestCase", "Found element '" + id + "' with text '" + text + "'");
                            }
                        }
                    );
                }
                return false;
            }

            @Override public void describeTo(Description description) {
                description.appendText("is a WebView containing an element with ID \""
                    + id + "\" and text \"" + text + "\"");
            }
        };
    }

    /** This matcher is designed only to be used repeatedly in a waitFor() call. */
    protected Matcher<View> containsNoElementWithId(String id) {
        return new TypeSafeMatcher<View>() {
            private View matchedView = null;

            @Override protected boolean matchesSafely(View view) {
                if (view == matchedView) return true;
                if (view instanceof WebView) {
                    ((WebView) view).evaluateJavascript(
                        "document.getElementById('" + id + "')",
                        result -> {
                            if (eq(result, "null")) {
                                matchedView = view;
                                Log.i("buendia/TestCase", "Confirmed that element '" + id + "' is not present");
                            }
                        }
                    );
                }
                return false;
            }

            @Override public void describeTo(Description description) {
                description.appendText("is a WebView that does not contain any element with ID \"" + id + "\"");
            }
        };
    }

    protected String getString(int id, Object... args) {
        return getActivity().getString(id, args);
    }
}
