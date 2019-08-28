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
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;

import org.projectbuendia.client.utils.Logger;

public class ShrinkFitTextView extends AppCompatTextView {
    private static final Logger LOG = Logger.create();
    private static final float MIN_SIZE_SP = 6;
    private static final float STEP_SP = 4;
    private float defaultTextSize = -1;
    private int fixedHeight = -1;

    public ShrinkFitTextView(Context context) {
        this(context, null);
    }

    public ShrinkFitTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public ShrinkFitTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSingleLine();
        setEllipsize(TextUtils.TruncateAt.END);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        if (defaultTextSize < 0) {
            defaultTextSize = getTextSize()/getResources().getDisplayMetrics().scaledDensity;
            CharSequence seq = getText();
            setText("8");
            super.onMeasure(widthSpec, heightSpec);
            fixedHeight = getMeasuredHeight();
            setText(seq);
        }
        // Start at the default size, and keep shrinking the text if it doesn't fit.
        for (float size = defaultTextSize; size > MIN_SIZE_SP; size -= STEP_SP) {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
            super.onMeasure(widthSpec, heightSpec);
            if (getLayout().getEllipsisCount(0) == 0) break;
        }
        // Maintain the height that this view had when the text was the default size.
        setHeight(fixedHeight);
    }

    public void setTextAndResize(String text) {
        super.setText(text);
        measure(0, 0);
    }
}
