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

package org.projectbuendia.client.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.projectbuendia.client.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** A compound view that displays a patient attribute name and value with optional icon. */
public class PatientAttributeView extends LinearLayout {

    @InjectView(R.id.view_attribute_name) TextView mNameView;
    @InjectView(R.id.view_attribute_value) TextView mValueView;
    @InjectView(R.id.view_attribute_icon) ImageView mImageView;

    public PatientAttributeView(Context context) {
        this(context, null);
    }

    public PatientAttributeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /** Constructs a {@link PatientAttributeView} with custom style parameters. */
    public PatientAttributeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(
            R.layout.view_patient_attribute, this, true /*attachToRoot*/);
        ButterKnife.inject(this);

        Resources resources = getResources();
        int defaultTextColor = resources.getColor(R.color.view_patient_attribute_text_color);

        TypedArray a = context.obtainStyledAttributes(
            attrs, R.styleable.PatientAttributeView, defStyleAttr, 0 /*defStyleRes*/);
        String value;
        String name;
        int textColor;
        int iconResource;
        try {
            textColor = a.getColor(
                R.styleable.PatientAttributeView_attributeTextColor, defaultTextColor);
            name = a.getString(R.styleable.PatientAttributeView_attributeName);
            value = a.getString(R.styleable.PatientAttributeView_attributeValue);
            iconResource = a.getInt(R.styleable.PatientAttributeView_attributeIconResource, 0);
        } finally {
            a.recycle();
        }

        mNameView.setTextColor(textColor);
        mNameView.setText(name);
        mValueView.setTextColor(textColor);
        mValueView.setText(value);
        if (iconResource == 0) {
            mImageView.setVisibility(GONE);
        } else {
            mImageView.setImageResource(iconResource);
        }
    }

    /** Sets the text in the name view. */
    public PatientAttributeView setName(CharSequence name) {
        mNameView.setText(name);

        return this;
    }

    /** Sets the text in the value view. */
    public PatientAttributeView setValue(CharSequence value) {
        mValueView.setText(value);

        return this;
    }

    /** Sets the text color of both the name and the value. */
    public PatientAttributeView setTextColor(int color) {
        mNameView.setTextColor(color);
        mValueView.setTextColor(color);

        return this;
    }

    /** Sets a {@link Drawable} for use as an icon. */
    public PatientAttributeView setIcon(Drawable drawable) {
        mImageView.setVisibility(VISIBLE);
        mImageView.setImageDrawable(drawable);
        return this;
    }
}
