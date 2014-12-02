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
import me.grantland.widget.AutofitTextView;

/**
 * A compound view that displays a patient vital in a colorful rectangle.
 */
public class VitalView extends LinearLayout {

    @InjectView(R.id.view_vital_name) TextView mNameView;
    @InjectView(R.id.view_vital_value) AutofitTextView mValueView;

    private int mTextColor;
    private String mName;
    private float mNameTextSize;
    private String mValue;
    private final int mVitalValueMaxLines;

    public VitalView(Context context) {
        this(context, null);
    }

    public VitalView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VitalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.view_vital, this, true /*attachToRoot*/);
        ButterKnife.inject(this);

        Resources resources = getResources();
        int defaultTextColor = resources.getColor(R.color.view_vital_text_color);
        float defaultNameTextSize = resources.getDimension(R.dimen.view_vital_name_text_size);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.VitalView, defStyleAttr, 0 /*defStyleRes*/);
        try {
            mTextColor = a.getColor(R.styleable.VitalView_textColor, defaultTextColor);
            mName = a.getString(R.styleable.VitalView_vitalName);
            mNameTextSize =
                    a.getDimension(R.styleable.VitalView_vitalNameTextSize, defaultNameTextSize);
            mValue = a.getString(R.styleable.VitalView_vitalValue);
            mVitalValueMaxLines = a.getInteger(R.styleable.VitalView_vitalValueMaxLines, 1);
        } finally {
            a.recycle();
        }

        mNameView.setTextColor(mTextColor);
//        mNameView.setTextSize(mNameTextSize);
        mNameView.setText(mName);
        mValueView.setTextColor(mTextColor);
        mValueView.setText(mValue);
        mValueView.setMaxLines(mVitalValueMaxLines);
    }

    public VitalView setValue(CharSequence value) {
        mValueView.setText(value);

        return this;
    }
}
