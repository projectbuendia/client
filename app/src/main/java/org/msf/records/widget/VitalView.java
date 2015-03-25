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

    @InjectView(R.id.view_vital_name) TextView mNameView;
    @InjectView(R.id.view_vital_value) AutoResizeTextView mValueView;

    public VitalView(Context context) {
        this(context, null);
    }

    public VitalView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Instantiates a {@link VitalView}.
     * {@see LinearLayout(Context, AttributeSet, int)}
     */
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

        mNameView.setTextColor(textColor);
        mNameView.setText(name);
        mValueView.setTextColor(textColor);
        mValueView.setText(value);
        mValueView.setMaxLines(vitalValueMaxLines);
    }

    /**
     * Sets the vital name, using the builder pattern.
     * @param name the name
     * @return this, with the specified name applied
     */
    public VitalView setName(CharSequence name) {
        mNameView.setText(name);

        return this;
    }

    /**
     * Sets the vital value, using the builder pattern.
     * @param value the value
     * @return this, with the specified value applied
     */
    public VitalView setValue(CharSequence value) {
        mValueView.setText(value);

        return this;
    }

    /**
     * Sets the text color of both the name and the value, using the builder pattern.
     * @param color the color resource
     * @return this, with the specified color applied
     */
    public VitalView setTextColor(int color) {
        mNameView.setTextColor(color);
        mValueView.setTextColor(color);

        return this;
    }
}
