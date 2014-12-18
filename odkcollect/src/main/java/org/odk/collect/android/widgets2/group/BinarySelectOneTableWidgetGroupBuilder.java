package org.odk.collect.android.widgets2.group;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;

import org.javarosa.core.model.Constants;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.model.PrepopulatableFields;
import org.odk.collect.android.widgets2.common.Appearance;
import org.odk.collect.android.widgets2.selectone.BinarySelectOneWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link WidgetGroupBuilder} that builds {@link TableWidgetGroup}s with a bunch of
 * {@link BinarySelectOneWidget}s.
 */
public class BinarySelectOneTableWidgetGroupBuilder implements
        WidgetGroupBuilder<TableWidgetGroup, BinarySelectOneTableWidgetGroupBuilder> {

    private static final String TAG = BinarySelectOneTableWidgetGroupBuilder.class.getName();

    private final Appearance mAppearance;
    private final List<BinarySelectOneWidget> mWidgets;

    public BinarySelectOneTableWidgetGroupBuilder(Appearance appearance) {
        mAppearance = appearance;
        mWidgets = new ArrayList<BinarySelectOneWidget>();
    }

    @Override
    public BinarySelectOneTableWidgetGroupBuilder createAndAddWidget(
            Context context,
            FormEntryPrompt prompt,
            Appearance appearance,
            boolean forceReadOnly,
            int id,
            PrepopulatableFields fields) {
        if (prompt.getControlType() != Constants.CONTROL_SELECT_ONE) {
            Log.w(
                    TAG,
                    "A non-select-one field was found under a binary select-one group. It has not "
                            + "been added.");
            return this;
        }

        BinarySelectOneWidget widget;
        try {
            widget = new BinarySelectOneWidget(context, prompt, appearance, forceReadOnly);
        } catch (IllegalArgumentException e) {
            Log.w(
                    TAG,
                    "A select-one field that is not binary was found under a binary select-one "
                            + "group. It has not been added.",
                    e);
            return this;
        }

        // TODO(dxchen): Remove this workaround.
        String questionText = prompt.getQuestionText().toLowerCase();
        if (questionText.equals("pregnant")) {
            widget.forceSetAnswer(fields.pregnant);
        }
        if (questionText.equals("iv access present")) {
            widget.forceSetAnswer(fields.ivFitted);
        }

        widget.setId(id);
        mWidgets.add(widget);

        return this;
    }

    @Override
    public TableWidgetGroup build(Context context) {
        TableWidgetGroup group = (TableWidgetGroup) LayoutInflater.from(context)
                .inflate(R.layout.template_binary_select_one_widget_group, null /*parent*/);

        for (BinarySelectOneWidget widget : mWidgets) {
            group.addView(widget);
        }

        return group;
    }
}
