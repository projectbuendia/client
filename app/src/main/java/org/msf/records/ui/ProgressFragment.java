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


/**
 * A simple {@link Fragment} subclass.
 *
 */
public abstract class ProgressFragment extends Fragment implements  Response.ErrorListener {

    public String TAG = ((Object) this).getClass().getSimpleName();

    public enum State {
        LOADING,
        LOADED,
        ERROR
    }

    protected View mContent;

    protected RelativeLayout mFrame;
    protected ProgressBar mProgressBar;
    protected TextView mErrorTextView;

    protected int mShortAnimationDuration;

    public ProgressFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
    }

    @Override
    public void onErrorResponse(VolleyError error){
        changeErrorState(error.toString());
        Log.e("server", new String(error.networkResponse.data, Charsets.UTF_8));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.getServer().cancelPendingRequests(TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        mProgressBar = new ProgressBar(getActivity());
        mProgressBar.setLayoutParams(relativeLayout);


        RelativeLayout.LayoutParams fullLayout = new RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        fullLayout.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mErrorTextView = new TextView(getActivity());
        mErrorTextView.setLayoutParams(fullLayout);
        mErrorTextView.setGravity(Gravity.CENTER);

        mContent.setVisibility(View.GONE);
        mFrame.addView(mProgressBar);
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

    protected void setContentView(int layout){
        mContent = LayoutInflater.from(getActivity()).inflate(layout, null, false);
    }

    protected void changeErrorState(String message){
        mErrorTextView.setText(message);
        changeState(State.ERROR);
    }

    protected void changeState(State state){
        mProgressBar.setVisibility(state != State.LOADED && state != State.ERROR ? View.VISIBLE : View.GONE);
        mContent.setVisibility(state != State.LOADING && state != State.ERROR ? View.VISIBLE : View.GONE);
        mErrorTextView.setVisibility(state != State.LOADING && state != State.LOADED ? View.VISIBLE : View.GONE);
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
        if(outView != null) {
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
