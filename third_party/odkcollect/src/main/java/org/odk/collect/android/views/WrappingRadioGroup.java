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

package org.odk.collect.android.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * WrappingRadioGroup is a {@link RadioGroup} that wraps when its choices would otherwise cause a
 * line to overflow.
 */
public class WrappingRadioGroup extends RadioGroup {
    /** Padding between container and radio buttons. */
    public static final int PADDING = 20;
    /** Padding between radio buttons. */
    public static final int BUTTON_PADDING = 7;

    public WrappingRadioGroup(Context context) {
        super(context);
    }

    public WrappingRadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setPadding(PADDING, PADDING, PADDING, PADDING);

        final int width = MeasureSpec.getSize(widthMeasureSpec);

        // Ensure that radio buttons are displayed on the correct lines.
        final int childCount = getChildCount();
        int x = PADDING;
        int y = PADDING;
        int lastButtonHeight = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            final int buttonWidth = getButtonWidth(child);
            final int buttonHeight = getButtonHeight(child);

            // If the button would overflow, go to the next line.
            if (x + buttonWidth > width) {
                x = PADDING;
                y += ((lastButtonHeight > 0) ? lastButtonHeight : buttonHeight) + BUTTON_PADDING;
            }

            // Go to next button position.
            x += buttonWidth;
            lastButtonHeight = buttonHeight;
        }

        // Height is the total height of all lines including top and bottom padding as well as
        // the bottom padding of the buttons on the last line.
        int newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                lastButtonHeight + y + PADDING + BUTTON_PADDING, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = getMeasuredWidth();

        // Ensure that radio buttons are displayed on the correct lines.
        final int childCount = getChildCount();
        int x = PADDING;
        int y = PADDING;
        int lastButtonHeight = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            final int buttonWidth = getButtonWidth(child);
            final int buttonHeight = getButtonHeight(child);

            // If the button would overflow, go to the next line.
            if (x + buttonWidth > width) {
                x = PADDING;
                y += ((lastButtonHeight > 0) ? lastButtonHeight : buttonHeight) + BUTTON_PADDING;
            }

            // Layout this button and go to the next position.
            child.layout(x, y, x + buttonWidth, y + buttonHeight);
            x += buttonWidth;
            lastButtonHeight = buttonHeight;
        }
    }

    /** Given an un-inflated button, gets the expected width. */
    private int getButtonWidth(View button) {
        TextView textView = (TextView)button;
        textView.measure(0, 0);
        return textView.getMeasuredWidth();
    }

    /** Given an un-inflated button, gets the expected height. */
    private int getButtonHeight(View button) {
        TextView textView = (TextView)button;
        textView.measure(0, 0);
        return textView.getMeasuredHeight();
    }
}
