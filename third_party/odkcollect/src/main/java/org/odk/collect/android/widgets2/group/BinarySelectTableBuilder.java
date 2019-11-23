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
import org.odk.collect.android.widgets2.selectone.BinarySelectWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link WidgetGroupBuilder} that builds a {@link TableWidgetGroup}
 * containing several {@link BinarySelectWidget}s.  In Buendia, this how we
 * present a set of choices that allows any number of choices to be selected.
 * Each choice can be in a "yes" state, "no" state, or unanswered state.
 * This builder is triggered by the appearance "binary-select-one" on the
 * group; see {@link WidgetGroupBuilderFactory}.
 *
 * We don't want to assume a "no" for choices that the user may have skipped
 * over; we wnat to require an explicit action to set the choices to "no".
 * So, in addition to the choices in the group, we offer a "None of the above"
 * button that can be tapped to explicitly specify "no" for all the choices.
 *
 * The group maintains the following invariant: either
 *
 * 1.  "None of the above" is selected and all choices are set to "no"; or
 *
 * 2.  "None of the above" is not selected and all the choices are either
 *     "yes" or "unanswered".
 *
 * For now, we hope to keep the UX simple by not providing a way to set
 * individual choices to "no" without setting them all to "no".  We also
 * don't provide a button to clear the entire group to "unanswered"; the same
 * effect can be achieved selecting and then deselecting "None of the above".
 */
public class BinarySelectTableBuilder implements
        WidgetGroupBuilder<TableWidgetGroup, BinarySelectTableBuilder>, View.OnClickListener {

    private static final String TAG = BinarySelectTableBuilder.class.getName();

    private final Appearance mAppearance;
    private final List<BinarySelectWidget> mWidgets;
    private CheckBox mNoneButton;

    public BinarySelectTableBuilder(Appearance appearance) {
        mAppearance = appearance;
        mWidgets = new ArrayList<BinarySelectWidget>();
    }

    @Override
    public BinarySelectTableBuilder createAndAddWidget(
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

        BinarySelectWidget widget;
        try {
            widget = new BinarySelectWidget(context, prompt, appearance, forceReadOnly);
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

        for (BinarySelectWidget widget : mWidgets) {
            widget.setOnClickCallback(this);
            group.addView(widget);
        }

        mNoneButton = createNoneButton(context, context.getString(R.string.none_of_the_above), group);
        group.addRow(mNoneButton);
        setTopMargin(context, mNoneButton, 12);
        return group;
    }

    /** Invoked after any choice is clicked. */
    @Override public void onClick(View view) {
        if (view instanceof BinarySelectWidget) {
            BinarySelectWidget widget = (BinarySelectWidget) view;
            // When any choices are set to "yes", all other choices revert to
            // the "unanswered" state instead of the "no" state.
            if (widget.getState() == true && mNoneButton.isChecked()) {
                mNoneButton.setChecked(false);
                setAllChoices(null);
                widget.setState(true);
            }
        }
    }

    private CheckBox createNoneButton(Context context, String label, ViewGroup parent) {
        CheckBox cb = (CheckBox) LayoutInflater.from(context).inflate(
            R.layout.template_check_box_button, parent, false);
        cb.setText(label);
        cb.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                setAllChoices(((CheckBox) v).isChecked() ? false : null);
            }
        });
        return cb;
    }

    private void setAllChoices(Boolean state) {
        for (BinarySelectWidget widget : mWidgets) {
            widget.setState(state);
        }
    }

    private void setTopMargin(Context context, View view, int marginSp) {
        MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        params.topMargin = (int) (marginSp * context.getResources().getDisplayMetrics().scaledDensity);
        view.setLayoutParams(params);
    }
}
