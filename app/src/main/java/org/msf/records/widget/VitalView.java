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
 * A compound view that displays a patient vital in a colorful rectangle.
 */
public class VitalView extends LinearLayout {

    // To keep the visual design calmer and less chaotic, the auto-sized text in
    // each tile on the patient chart uses one of a small set of sizes that are
    // used consistently across the entire app.  See res/values/buendia_styles.xml
    // for the canonical set of sizes that this should match.
    final static int[] ALLOWED_TEXT_SIZES = {17, 22, 28, 36, 45};

    @InjectView(R.id.view_vital_name) TextView mNameView;
    @InjectView(R.id.view_vital_value) AutoResizeTextView mValueView;

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

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.VitalView, defStyleAttr, 0 /*defStyleRes*/);
        int vitalValueMaxLines;
        String value;
        String name;
        int textColor;
        try {
            textColor = a.getColor(R.styleable.VitalView_textColor, defaultTextColor);
            name = a.getString(R.styleable.VitalView_vitalName);
            value = a.getString(R.styleable.VitalView_vitalValue);
            vitalValueMaxLines = a.getInteger(R.styleable.VitalView_vitalValueMaxLines, 1);
        } finally {
            a.recycle();
        }

        mValueView.setAllowedTextSizes(ALLOWED_TEXT_SIZES);

        mNameView.setTextColor(textColor);
        mNameView.setText(name);
        mValueView.setTextColor(textColor);
        mValueView.setText(value);
        mValueView.setMaxLines(vitalValueMaxLines);
    }

    /**
     * Sets the name.
     */
    public VitalView setName(CharSequence name) {
        mNameView.setText(name);

        return this;
    }

    public VitalView setValue(CharSequence value) {
        mValueView.setText(value);

        return this;
    }

    /**
     * Sets the text color of both the name and the value.
     */
    public VitalView setTextColor(int color) {
        mNameView.setTextColor(color);
        mValueView.setTextColor(color);

        return this;
    }
}
