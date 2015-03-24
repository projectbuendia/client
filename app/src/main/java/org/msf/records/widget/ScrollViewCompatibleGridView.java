package org.msf.records.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * A GridView that shows all its children when given unbounded space, so it can exist sensibly in
 * a {@link android.widget.ScrollView}.
 */
public class ScrollViewCompatibleGridView extends GridView {

    public ScrollViewCompatibleGridView(Context context) {
        super(context);
    }

    public ScrollViewCompatibleGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollViewCompatibleGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            // This number would normally be negative, but because makeMeasureSpec masks off the
            // most significant bits to store the mode, it ends up being the largest possible number
            // that we can provide
            heightMeasureSpec =
                    MeasureSpec.makeMeasureSpec(0xFFFFFFFF, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
