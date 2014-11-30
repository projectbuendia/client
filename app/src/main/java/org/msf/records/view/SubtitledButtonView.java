package org.msf.records.view;

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
 * Created by akalachman on 11/30/14.
 */
public class SubtitledButtonView extends LinearLayout {

    @InjectView(R.id.view_subtitled_button_subtitle)
    TextView mSubtitleView;
    @InjectView(R.id.view_subtitled_button_title)
    TextView mTitleView;

    private int mTextColor;
    private int mSubtitleTextColor;
    private String mTitle;
    private float mTitleTextSize;
    private String mSubtitle;
    private String mSubtitleFormat;

    public SubtitledButtonView(Context context) {
        this(context, null);
    }

    public SubtitledButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SubtitledButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(
                R.layout.view_subtitled_button, this, true /*attachToRoot*/);
        ButterKnife.inject(this);

        Resources resources = getResources();
        int defaultTextColor = resources.getColor(R.color.view_subtitled_button_text_color);
        int defaultSubtitleTextColor = resources.getColor(
                R.color.view_subtitled_button_subtitle_text_color);
        float defaultTitleTextSize = resources.getDimension(
                R.dimen.view_subtitled_button_title_text_size);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SubtitledButtonView, defStyleAttr, 0 /*defStyleRes*/);
        try {
            mTextColor = a.getColor(
                    R.styleable.SubtitledButtonView_subtitledButtonTextColor, defaultTextColor);
            mSubtitleTextColor = a.getColor(
                    R.styleable.SubtitledButtonView_subtitledButtonSubtitleTextColor,
                    defaultSubtitleTextColor);
            mTitle = a.getString(R.styleable.SubtitledButtonView_subtitledButtonTitle);
            mTitleTextSize = a.getDimension(
                    R.styleable.SubtitledButtonView_subtitledButtonTitleTextSize,
                    defaultTitleTextSize);
            mSubtitle = a.getString(R.styleable.SubtitledButtonView_subtitledButtonSubtitle);
            mSubtitleFormat = a.getString(
                    R.styleable.SubtitledButtonView_subtitledButtonSubtitleFormat);
        } finally {
            a.recycle();
        }

        mTitleView.setTextColor(mTextColor);
        mTitleView.setTextSize(mTitleTextSize);
        mTitleView.setText(mTitle);
        mSubtitleView.setTextColor(mSubtitleTextColor);
        mSubtitleView.setText(mSubtitle);

        mTitleView.setText(mTitle);
    }

    public SubtitledButtonView setValue(CharSequence value) {
        mSubtitleView.setText(value);

        return this;
    }
}
