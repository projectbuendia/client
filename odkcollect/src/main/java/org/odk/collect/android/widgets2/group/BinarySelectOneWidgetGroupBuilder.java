package org.odk.collect.android.widgets2.group;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;

import org.javarosa.core.model.Constants;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.widgets2.common.Appearance;
import org.odk.collect.android.widgets2.selectone.BinarySelectOneWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link WidgetGroupBuilder} that builds {@link BinarySelectOneWidgetGroup}s.
 */
public class BinarySelectOneWidgetGroupBuilder implements
        WidgetGroupBuilder<BinarySelectOneWidgetGroup, BinarySelectOneWidgetGroupBuilder> {

    private static final String TAG = BinarySelectOneWidgetGroupBuilder.class.getName();

    private final Appearance mAppearance;
    private final List<BinarySelectOneWidget> mWidgets;

    public BinarySelectOneWidgetGroupBuilder(Appearance appearance) {
        mAppearance = appearance;
        mWidgets = new ArrayList<BinarySelectOneWidget>();
    }

    @Override
    public BinarySelectOneWidgetGroupBuilder createAndAddWidget(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        if (prompt.getControlType() != Constants.CONTROL_SELECT_ONE) {
            Log.w(
                    TAG,
                    "A non-select-one field was found under a binary select-one group. It has not "
                            + "been added.");
            return this;
        }

        mWidgets.add(new BinarySelectOneWidget(context, prompt, appearance, forceReadOnly));

        return this;
    }

    @Override
    public BinarySelectOneWidgetGroup build(Context context) {
        BinarySelectOneWidgetGroup group = (BinarySelectOneWidgetGroup) LayoutInflater.from(context)
                .inflate(R.layout.template_binary_select_one_widget_group, null /*parent*/);

        for (BinarySelectOneWidget widget : mWidgets) {
            group.addView(widget);
        }

        return group;
    }
}
