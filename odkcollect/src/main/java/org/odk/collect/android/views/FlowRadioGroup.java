package org.odk.collect.android.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioGroup;

/**
 * A {@link RadioGroup} that wraps when its choices overflow a line.
 *
 * <p>Based on http://stackoverflow.com/a/560958. License is CC-by-SA v2.5.
 *
 * TODO: Figure out license.
 */
public class FlowRadioGroup extends RadioGroup {

    public FlowRadioGroup(Context context) {
        super(context);
    }

    public FlowRadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);

        // Add some padding to the children
        setPadding( 20, 20, 20, 20 );

        // The next line is WRONG!!! Doesn't take into account requested MeasureSpec mode!
        int height = getPaddingTop() + getPaddingBottom();

        final int count = getChildCount();

        int xpos = getPaddingLeft();
        int childrenSameHeightCount = 0;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                child.measure(
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));

                final int childw = child.getMeasuredWidth();

                if (xpos + childw > width) {
                    xpos = getPaddingLeft();

                    View heightChild = getChildAt( i - ( i == 0 ? 0 : 1) );
                    height += heightChild.getMeasuredHeight() + getPaddingBottom();
                    childrenSameHeightCount = 0;
                }

                else
                {
                    ++childrenSameHeightCount;
                }

                xpos += childw + lp.width;
            }
        }

        if ( count > 0 )
        {
            View heightChild = getChildAt( count - 1 );

            height += heightChild.getMeasuredHeight() + getPaddingBottom() + heightChild.getLayoutParams().height;
        }

        setMeasuredDimension(width, height );
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(1, 1); // default of 1px spacing
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        final int width = r - l;
        int xpos = getPaddingLeft();
        int ypos = getPaddingTop();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int childw = child.getMeasuredWidth();
                final int childh = child.getMeasuredHeight();
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (xpos + childw > width) {
                    xpos = getPaddingLeft();
                    ypos += getChildAt(i-1).getMeasuredHeight()+getPaddingBottom();
                }
                child.layout(xpos, ypos, xpos + childw, ypos + childh);
                xpos += childw + lp.width;
            }
        }
    }
}
