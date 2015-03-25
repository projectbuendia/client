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

package org.msf.records.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.base.Charsets;

import org.msf.records.App;
import org.msf.records.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Fragment} that shows a spinner or progress bar when fragment content is not ready to
 * be displayed.
 */
public abstract class ProgressFragment extends Fragment implements  Response.ErrorListener {

    public enum State {
        LOADING,
        LOADED,
        ERROR
    }

    protected View mContent;

    protected RelativeLayout mFrame;
    protected TextView mErrorTextView;

    // Fancy progress bar.
    protected View mProgressBarLayout;
    protected ProgressBar mProgressBar;
    protected TextView mProgressBarLabel;

    // Indeterminate progress bar.
    protected ProgressBar mIndeterminateProgressBar;

    protected int mShortAnimationDuration;

    private State mState = State.LOADING;
    private List<ChangeStateSubscriber> mSubscribers = new ArrayList<ChangeStateSubscriber>();

    public ProgressFragment() {
    }

    /** Subscriber for listening for state changes. */
    public interface ChangeStateSubscriber {
        /** Called whenever the state is changed. */
        public void onChangeState(State newState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        changeErrorState(error.toString());
        Log.e("server", new String(error.networkResponse.data, Charsets.UTF_8));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.getServer().cancelPendingRequests();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mFrame = new RelativeLayout(getActivity());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        mFrame.setLayoutParams(layoutParams);

        RelativeLayout.LayoutParams relativeLayout = new RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        relativeLayout.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mIndeterminateProgressBar = new ProgressBar(getActivity());
        mIndeterminateProgressBar.setLayoutParams(relativeLayout);

        mProgressBarLayout =
                inflater.inflate(R.layout.progress_fragment_measured_progress_view, null);
        mProgressBarLayout.setLayoutParams(relativeLayout);
        mProgressBar =
                (ProgressBar)mProgressBarLayout.findViewById(R.id.progress_fragment_progress_bar);
        mProgressBarLabel = (TextView)mProgressBarLayout.findViewById(R.id.progress_fragment_label);

        RelativeLayout.LayoutParams fullLayout = new RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        fullLayout.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mErrorTextView = new TextView(getActivity());
        mErrorTextView.setLayoutParams(fullLayout);
        mErrorTextView.setGravity(Gravity.CENTER);

        mContent.setVisibility(View.GONE);
        mProgressBarLayout.setVisibility(View.GONE);
        mFrame.addView(mIndeterminateProgressBar);
        mFrame.addView(mProgressBarLayout);
        mFrame.addView(mContent);
        mFrame.addView(mErrorTextView);
        return mFrame;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mFrame != null) {
            mFrame.removeAllViewsInLayout();
        }
    }

    /** Registers a {@link ChangeStateSubscriber}. */
    public void registerSubscriber(ChangeStateSubscriber subscriber) {
        mSubscribers.add(subscriber);
    }

    /** Unregisters a {@link ChangeStateSubscriber} if the subscriber is currently registered. */
    public void unregisterSubscriber(ChangeStateSubscriber subscriber) {
        if (mSubscribers.contains(subscriber)) {
            mSubscribers.remove(subscriber);
        }
    }

    public State getState() {
        return mState;
    }

    protected void setContentView(int layout) {
        mContent = LayoutInflater.from(getActivity()).inflate(layout, null, false);
    }

    protected void changeErrorState(String message) {
        mErrorTextView.setText(message);
        changeState(State.ERROR);
    }

    /**
     * Changes the state of this fragment, hiding or showing the spinner as necessary.
     */
    public void changeState(State state) {
        mState = state;
        // On state change, always start with the indeterminate loader.
        mProgressBarLayout.setVisibility(View.GONE);
        mIndeterminateProgressBar.setVisibility(state == State.LOADING ? View.VISIBLE : View.GONE);
        mContent.setVisibility(state == State.LOADED ? View.VISIBLE : View.GONE);
        mErrorTextView.setVisibility(state == State.ERROR ? View.VISIBLE : View.GONE);
        for (ChangeStateSubscriber subscriber : mSubscribers) {
            subscriber.onChangeState(mState);
        }
    }

    protected void incrementProgressBy(int progress) {
        switchToHorizontalProgressBar();
        mProgressBar.incrementProgressBy(progress);
    }

    protected void setProgress(int progress) {
        switchToHorizontalProgressBar();
        mProgressBar.setProgress(progress);
    }

    protected void setProgressLabel(String label) {
        switchToHorizontalProgressBar();
        mProgressBarLabel.setText(label);
    }

    protected void switchToHorizontalProgressBar() {
        if (mState == State.LOADING) {
            mIndeterminateProgressBar.setVisibility(View.GONE);
            mProgressBarLayout.setVisibility(View.VISIBLE);
        }
    }

    protected void switchToCircularProgressBar() {
        if (mState == State.LOADING) {
            mIndeterminateProgressBar.setVisibility(View.VISIBLE);
            mProgressBarLayout.setVisibility(View.GONE);
        }
    }

    private void crossfade(View inView, final View outView) {

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        inView.setAlpha(0f);
        inView.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        inView.animate()
            .alpha(1f)
            .setDuration(mShortAnimationDuration)
            .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        if (outView != null) {
            outView.animate()
                    .alpha(0f)
                    .setDuration(mShortAnimationDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            outView.setVisibility(View.GONE);
                        }
                    });
        }
    }

}
