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

package org.projectbuendia.client.ui;

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

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/** A Fragment that shows a spinner or progress bar when content is not ready. */
public abstract class ProgressFragment extends Fragment implements Response.ErrorListener {

    private static final Logger LOG = Logger.create();
    protected int mContentLayout;
    protected View mContent;
    protected RelativeLayout mFrame;
    protected TextView mErrorTextView;

    protected View mProgressBarLayout;
    protected ProgressBar mProgressBar;
    protected TextView mProgressBarLabel;
    protected ProgressBar mIndeterminateProgressBar;

    protected int mShortAnimationDuration;
    private ReadyState mState = ReadyState.LOADING;
    private List<ReadyStateSubscriber> mSubscribers = new ArrayList<>();

    public interface ReadyStateSubscriber {
        public void onChangeState(ReadyState state);
    }

    public ProgressFragment() {
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mShortAnimationDuration = getResources().getInteger(
            android.R.integer.config_shortAnimTime);
    }

    @Override public void onErrorResponse(VolleyError error) {
        setErrorState(error.toString());
        Log.e("server", new String(error.networkResponse.data, Charsets.UTF_8));
    }

    public void setErrorState(String message) {
        mErrorTextView.setText(message);
        setReadyState(ReadyState.ERROR);
    }

    /** Changes the state of this fragment, hiding or showing the spinner as necessary. */
    public void setReadyState(ReadyState state) {
        if (state == mState) return;
        LOG.w("setReadyState %s", state);

        mState = state;
        mProgressBarLayout.setVisibility(state == ReadyState.SYNCING ? View.VISIBLE : View.GONE);
        mIndeterminateProgressBar.setVisibility(state == ReadyState.LOADING ? View.VISIBLE : View.GONE);
        mContent.setVisibility(state == ReadyState.READY ? View.VISIBLE : View.GONE);
        mErrorTextView.setVisibility(state == ReadyState.ERROR ? View.VISIBLE : View.GONE);
        for (ReadyStateSubscriber subscriber : mSubscribers) {
            subscriber.onChangeState(mState);
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        App.getServer().cancelPendingRequests();
    }

    @Override public View onCreateView(
        LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mFrame = new RelativeLayout(getActivity());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT);
        mFrame.setLayoutParams(layoutParams);

        RelativeLayout.LayoutParams centeredLayout = new RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        centeredLayout.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mIndeterminateProgressBar = new ProgressBar(getActivity());
        mIndeterminateProgressBar.setLayoutParams(centeredLayout);

        mProgressBarLayout =
            inflater.inflate(R.layout.progress_fragment_measured_progress_view, null);
        mProgressBarLayout.setLayoutParams(centeredLayout);
        mProgressBar =
            mProgressBarLayout.findViewById(R.id.progress_fragment_progress_bar);
        mProgressBarLabel = mProgressBarLayout.findViewById(R.id.progress_fragment_label);
        mProgressBarLabel.setText(R.string.sync_in_progress);

        RelativeLayout.LayoutParams fullLayout = new RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT);
        fullLayout.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mErrorTextView = new TextView(getActivity());
        mErrorTextView.setLayoutParams(fullLayout);
        mErrorTextView.setGravity(Gravity.CENTER);

        mFrame.setLayoutParams(fullLayout);

        mContent = LayoutInflater.from(getActivity()).inflate(mContentLayout, mFrame, false);
        mContent.setVisibility(View.GONE);

        mIndeterminateProgressBar.setVisibility(View.VISIBLE);
        mProgressBarLayout.setVisibility(View.GONE);
        mErrorTextView.setVisibility(View.GONE);

        mFrame.addView(mIndeterminateProgressBar);
        mFrame.addView(mProgressBarLayout);
        mFrame.addView(mContent);
        mFrame.addView(mErrorTextView);
        return mFrame;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (mFrame != null) {
            mFrame.removeAllViewsInLayout();
        }
    }

    public void registerSubscriber(ReadyStateSubscriber subscriber) {
        mSubscribers.add(subscriber);
    }

    public void unregisterSubscriber(ReadyStateSubscriber subscriber) {
        mSubscribers.remove(subscriber);
    }

    public ReadyState getState() {
        return mState;
    }

    protected void setContentLayout(int layout) {
        mContentLayout = layout;
    }

    protected void setProgress(int numerator, int denominator) {
        if (denominator > 0) mProgressBar.setMax(denominator);
        mProgressBar.setProgress(numerator);
    }

    protected void setProgressMessage(int messageId) {
        mProgressBarLabel.setText(messageId);
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
                    @Override public void onAnimationEnd(Animator animation) {
                        outView.setVisibility(View.GONE);
                    }
                });
        }
    }

}
