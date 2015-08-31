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

package org.projectbuendia.client.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.projectbuendia.client.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** A button with a subtitle. */
public class SubtitledButtonView extends LinearLayout {

    @InjectView(R.id.view_subtitled_button_subtitle)
    TextView mSubtitleView;
    @InjectView(R.id.view_subtitled_button_title)
    TextView mTitleView;

    private boolean mIsSquare;

    public SubtitledButtonView(Context context) {
        this(context, null);
    }

    public SubtitledButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Instantiates and inflates a {@link SubtitledButtonView}.
     * {@see LinearLayout(Context, AttributeSet, int)}
     */
    public SubtitledButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO: Find a way to maintain the border even when setting a bgcolor.
        setBackgroundResource(R.drawable.border_grey_1dp);

        LayoutInflater.from(context).inflate(
                R.layout.view_subtitled_button, this, true /*attachToRoot*/);
        ButterKnife.inject(this);

        Resources resources = getResources();
        int defaultTextColor = resources.getColor(R.color.view_subtitled_button_text_color);
        int defaultSubtitleTextColor = resources.getColor(
                R.color.view_subtitled_button_subtitle_text_color);
        boolean defaultIsSquare = resources.getBoolean(
                R.bool.view_subtitled_button_is_square);
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SubtitledButtonView, defStyleAttr, 0 /*defStyleRes*/);
        String subtitle;
        String title;
        int subtitleTextColor;
        int textColor;
        try {
            textColor = a.getColor(
                    R.styleable.SubtitledButtonView_subtitledButtonTextColor, defaultTextColor);
            subtitleTextColor = a.getColor(
                    R.styleable.SubtitledButtonView_subtitledButtonSubtitleTextColor,
                    defaultSubtitleTextColor);
            title = a.getString(R.styleable.SubtitledButtonView_subtitledButtonTitle);
            subtitle = a.getString(R.styleable.SubtitledButtonView_subtitledButtonSubtitle);
            mIsSquare = a.getBoolean(R.styleable.SubtitledButtonView_subtitledButtonIsSquare,
                    defaultIsSquare);
        } finally {
            a.recycle();
        }

        mTitleView.setTextColor(textColor);
        mTitleView.setText(title);
        mSubtitleView.setTextColor(subtitleTextColor);
        mSubtitleView.setText(subtitle);
    }

    /** Sets the text color of both the title and the subtitle. */
    public SubtitledButtonView setTextColor(int color) {
        mTitleView.setTextColor(color);
        mSubtitleView.setTextColor(color);
        return this;
    }

    /** Sets the text color of the subtitle. */
    public SubtitledButtonView setSubtitleColor(int color) {
        mSubtitleView.setTextColor(color);
        return this;
    }

    /**
     * Sets the title of the button, using the builder pattern.
     * @param title the title text
     * @return the button with the specified title
     */
    public SubtitledButtonView setTitle(CharSequence title) {
        mTitleView.setText(title);

        return this;
    }

    /**
     * Sets the subtitle of the button, using the builder pattern.
     * @param subtitle the subtitle text
     * @return the button with the specified subtitle
     */
    public SubtitledButtonView setSubtitle(CharSequence subtitle) {
        mSubtitleView.setText(subtitle);

        return this;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!mIsSquare) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int size = width > height ? height : width;
        setMeasuredDimension(size, size);
    }
}
