package org.odk.collect.android.widgets2.group;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridLayout;
import org.odk.collect.android.widgets2.selectone.BinarySelectOneWidget;

/**
 * A {@link WidgetGroup} that contains a number of {@link BinarySelectOneWidget}s.
 */
public class BinarySelectOneWidgetGroup extends GridLayout implements WidgetGroup {

    public BinarySelectOneWidgetGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
}
