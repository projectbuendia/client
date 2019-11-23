package org.odk.collect.android.widgets2.group;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.CheckBox;

import org.javarosa.core.model.Constants;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.model.Preset;
import org.odk.collect.android.widgets2.common.Appearance;
import org.odk.collect.android.widgets2.selectone.BinarySelectOneWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link WidgetGroupBuilder} that builds a {@link TableWidgetGroup}
 * containing several {@link BinarySelectOneWidget}s.  In Buendia, this
 * is the way we present a set of choices that allows any number of choices
 * to be selected.  This is logically equivalent to a group of checkboxes,
 * but JavaRosa has no concept of a boolean control type; booleans are
 * represented as select-one controls with "yes" and "no" as the two choices.
 * This builder is triggered by the appearance "binary-select-one" on the
 * group; see {@link WidgetGroupBuilderFactory}.
 */
public class BinarySelectOneTableWidgetGroupBuilder implements
        WidgetGroupBuilder<TableWidgetGroup, BinarySelectOneTableWidgetGroupBuilder>, View.OnClickListener {

    private static final String TAG = BinarySelectOneTableWidgetGroupBuilder.class.getName();

    private final Appearance mAppearance;
    private final List<BinarySelectOneWidget> mWidgets;
    private CheckBox mNoneButton;

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
            Preset preset) {
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

        // TODO: Remove this workaround.
        String questionText = prompt.getQuestionText().toLowerCase();
        if (questionText.equals("pregnant")) {
            widget.forceSetAnswer(preset.pregnancy);
        }
        if (questionText.equals("iv access present")) {
            widget.forceSetAnswer(preset.ivAccess);
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
            widget.setOnClickCallback(this);
            group.addView(widget);
        }

        mNoneButton = createNoneButton(context, context.getString(R.string.none_of_the_above), group);
        group.addRow(mNoneButton);
        setTopMargin(context, mNoneButton, 12);
        return group;
    }

    @Override public void onClick(View view) {
        if (view instanceof BinarySelectOneWidget) {
            BinarySelectOneWidget widget = (BinarySelectOneWidget) view;
            if (widget.getState() == true) {
                mNoneButton.setChecked(false);
            }
        }
    }

    private CheckBox createNoneButton(Context context, String label, ViewGroup parent) {
        CheckBox cb = (CheckBox) LayoutInflater.from(context).inflate(
            R.layout.template_check_box_button, parent, false);
        cb.setText(label);
        cb.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Boolean newState = ((CheckBox) v).isChecked() ? false : null;
                for (BinarySelectOneWidget widget : mWidgets) {
                    widget.setState(newState);
                }
            }
        });
        return cb;
    }

    private void setTopMargin(Context context, View view, int marginSp) {
        MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        params.topMargin = (int) (marginSp * context.getResources().getDisplayMetrics().scaledDensity);
        view.setLayoutParams(params);
    }
}
