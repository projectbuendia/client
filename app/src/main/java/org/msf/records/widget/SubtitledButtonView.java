package org.msf.records.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.msf.records.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * A button with a subtitle.
 */
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

    public SubtitledButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO(akalachman): Find a way to maintain the border even when setting a bgcolor.
        setBackgroundResource(R.drawable.border_grey_1dp);

        LayoutInflater.from(context).inflate(
                R.layout.view_subtitled_button, this, true /*attachToRoot*/);
        ButterKnife.inject(this);

        Resources resources = getResources();
        int defaultTextColor = resources.getColor(R.color.view_subtitled_button_text_color);
        int defaultSubtitleTextColor = resources.getColor(
                R.color.view_subtitled_button_subtitle_text_color);
        float defaultTitleTextSize = resources.getDimension(
                R.dimen.view_subtitled_button_title_text_size);
        boolean defaultIsSquare = resources.getBoolean(
                R.bool.view_subtitled_button_is_square);
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SubtitledButtonView, defStyleAttr, 0 /*defStyleRes*/);
        String subtitle;
        float titleTextSize;
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
            titleTextSize = a.getDimension(
                    R.styleable.SubtitledButtonView_subtitledButtonTitleTextSize,
                    defaultTitleTextSize);
            subtitle = a.getString(R.styleable.SubtitledButtonView_subtitledButtonSubtitle);
            mIsSquare = a.getBoolean(R.styleable.SubtitledButtonView_subtitledButtonIsSquare,
                    defaultIsSquare);
        } finally {
            a.recycle();
        }

        mTitleView.setTextColor(textColor);
        mTitleView.setTextSize(titleTextSize);
        mTitleView.setText(title);
        mSubtitleView.setTextColor(subtitleTextColor);
        mSubtitleView.setText(subtitle);
    }

    public SubtitledButtonView setTextColor(int colorResource) {
        int color = getContext().getResources().getColor(colorResource);
        mTitleView.setTextColor(color);
        mSubtitleView.setTextColor(color);

        return this;
    }

    public SubtitledButtonView setTitle(CharSequence title) {
        mTitleView.setText(title);

        return this;
    }

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
