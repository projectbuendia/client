package org.projectbuendia.client.acceptance;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.espresso.matcher.ViewMatchers;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.is;

public class ListItemCountAssertion implements ViewAssertion {
    private final Matcher<Integer> matcher;

    public static ListItemCountAssertion hasItemCount(Matcher<Integer> matcher) {
        return new ListItemCountAssertion(matcher);
    }

    public static ListItemCountAssertion hasItemCount(int count) {
        return new ListItemCountAssertion(is(count));
    }

    public ListItemCountAssertion(Matcher<Integer> matcher) {
        this.matcher = matcher;
    }

    @Override public void check(View view, NoMatchingViewException noViewFoundException) {
        if (noViewFoundException != null) {
            throw noViewFoundException;
        }
        ListAdapter adapter = ((AdapterView<ListAdapter>) view).getAdapter();
        ViewMatchers.assertThat(adapter.getCount(), matcher);
    }
}
