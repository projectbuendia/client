package org.msf.records.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
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
    private boolean mIsSquare;

    public SubtitledButtonView(Context context) {
        this(context, null);
    }

    public SubtitledButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SubtitledButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
            mIsSquare = a.getBoolean(R.styleable.SubtitledButtonView_subtitledButtonIsSquare,
                    defaultIsSquare);
        } finally {
            a.recycle();
        }

        mTitleView.setTextColor(mTextColor);
        mTitleView.setTextSize(mTitleTextSize);
        mTitleView.setText(mTitle);
        mSubtitleView.setTextColor(mSubtitleTextColor);
        mSubtitleView.setText(mSubtitle);
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
