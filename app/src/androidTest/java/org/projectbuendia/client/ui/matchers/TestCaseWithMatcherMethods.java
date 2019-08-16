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
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.espresso.web.model.Atom;
import android.support.test.espresso.web.model.ElementReference;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import com.estimote.sdk.internal.Preconditions;
import com.google.common.base.Joiner;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;

import java.text.MessageFormat;

import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webContent;
import static android.support.test.espresso.web.matcher.DomMatchers.elementByXPath;
import static android.support.test.espresso.web.matcher.DomMatchers.hasElementWithXpath;
import static android.support.test.espresso.web.matcher.DomMatchers.withTextContent;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.Matchers.allOf;

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
            "has the exact text \"" + text + "\"");
    }

    public static ViewInteraction viewWithTextString(int resourceId) {
        return Espresso.onView(hasTextString(resourceId));
    }

    public static Matcher<View> hasTextString(int resourceId) {
        return new MatcherWithDescription<>(ViewMatchers.withText(resourceId),
            "has string resource " + resourceId + " as its text");
    }

    public static ViewInteraction firstViewThat(Matcher<View>... matchers) {
        final Matcher matcher = matchers.length > 1 ? allOf(matchers) : matchers[0];
        return Espresso.onView(new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("is first match");
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                return matcher.matches(view) && currentIndex++ == 0;
            }
        });
    }

    @SafeVarargs
    public static ViewInteraction viewThat(Matcher<View>... matchers) {
        return Espresso.onView(matchers.length > 1 ? allOf(matchers) : matchers[0]);
    }

    public static DataInteraction dataThat(Matcher... matchers) {
        return Espresso.onData(matchers.length > 1 ? allOf(matchers) : matchers[0]);
    }

    @SafeVarargs
    public static void expect(ViewInteraction vi, Matcher<View>... matchers) {
        vi.check(matches(matchers.length == 0 ? isVisible() :
            matchers.length > 1 ? allOf(matchers) : matchers[0]));
    }

    public static Matcher<View> isVisible() {
        return new MatcherWithDescription<>(ViewMatchers.isDisplayed(),
            "is visible");
    }

    public static void expectVisible(DataInteraction di) {
        di.check(matches(isVisible()));
    }

    public static void click(DataInteraction di) {
        di.perform(ViewActions.click());
    }

    public static void openActionBarOptionsMenu() {
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getTargetContext());
    }

    public static void scrollToAndClick(ViewInteraction vi) {
        scrollTo(vi);
        click(vi);
    }

    public static void scrollTo(ViewInteraction vi) {
        vi.perform(ViewActions.scrollTo());
    }

    public static void click(ViewInteraction vi) {
        vi.perform(ViewActions.click());
    }

    public static void act(ViewInteraction vi, ViewAction action) {
        vi.perform(action);
    }

    public static void scrollToAndType(Object obj, ViewInteraction vi) {
        scrollTo(vi);
        type(obj, vi);
    }

    public static void type(Object obj, ViewInteraction vi) {
        vi.perform(ViewActions.typeText(obj.toString()));
    }

    public static void clearAndType(Object obj, ViewInteraction vi) {
        vi.perform(ViewActions.clearText()).perform(ViewActions.typeText(obj.toString()));
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

    public static void click(Atom<ElementReference> atom) {
        onWebView().withElement(atom).perform(webClick());
    }

    public static void clickXpath(String xpath) {
        click(findElement(Locator.XPATH, xpath));
    }

    public static void expectXpath(String xpath) {
        onWebView().check(webContent(hasElementWithXpath(xpath)));
    }

    public static void expectXpathText(String xpath, Matcher<String> matcher) {
        onWebView().check(webContent(elementByXPath(xpath, withTextContent(matcher))));
    }

    public static void expectXpathNotFound(String xpath) {
        try {
            expectXpath(xpath);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return;
        }
        throw new AssertionError("XPath unexpectedly present: " + xpath);
    }

    // Matchers with better descriptions than those in espresso.matcher.ViewMatchers.

    public static void scrollToAndExpectVisible(ViewInteraction vi) {
        scrollTo(vi);
        vi.check(matches(isVisible()));
    }

    public static Matcher<View> isA(final Class<? extends View> cls) {
        String name = cls.getSimpleName();
        return new MatcherWithDescription<>(
            ViewMatchers.isAssignableFrom(cls),
            (name.matches("^[AEIOU]") ? "is an " : "is a ") + name);
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
                    list = list.replaceAll(", ([^,*])$", ", or $1");
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
            "has text containing \"" + text + "\"");
    }

    public static Matcher<View> hasTextMatchingRegex(String regex) {
        return new MatcherWithDescription<>(
            ViewMatchers.withText(StringMatchers.matchesRegex(regex)),
            "has text matching regex /" + regex + "/");
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

    protected static void expectVisibleSoon(ViewInteraction vi) {
        expectVisibleWithin(30000, vi);
    }

    protected static void expectVisibleWithin(int timeoutMs, ViewInteraction vi) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        boolean found = false;
        Throwable throwable = null;
        while (!found && System.currentTimeMillis() < deadline) {
            try {
                expectVisible(vi);
                found = true;
            } catch (Throwable t) {
                throwable = t;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    Thread.yield();
                }
            }
        }
        if (!found) {
            throw new RuntimeException(throwable);
        }
    }

    public static void expectVisible(ViewInteraction vi) {
        vi.check(matches(isVisible()));
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
}
