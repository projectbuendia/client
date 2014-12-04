package org.odk.collect.android.widgets2.selectone;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.RadioButton;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.views.FlowRadioGroup;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets2.common.Appearance;
import org.odk.collect.android.widgets2.common.TypedWidget;

import java.util.List;

/**
 * A {@link SelectOneData} {@link TypedWidget} that displays choices as a flow layout of buttons.
 */
public class ButtonsSelectOneWidget extends TypedWidget<SelectOneData> {

    private final List<SelectChoice> mChoices;
    private final FlowRadioGroup mGroup;

    public ButtonsSelectOneWidget(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        // TODO(dxchen): Handle initial values.

        super(context, prompt, appearance, forceReadOnly);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        mGroup = (FlowRadioGroup) inflater.inflate(R.layout.template_segmented_group, null);

        mChoices = prompt.getSelectChoices();
        String defaultAnswer = prompt.getAnswerValue() == null
                ? null
                : ((Selection) prompt.getAnswerValue().getValue()).getValue();

        boolean isReadOnly = forceReadOnly || prompt.isReadOnly();

        for (int i = 0; i < mChoices.size(); i++) {
            SelectChoice choice = mChoices.get(i);

            RadioButton radioButton =
                    (RadioButton) inflater.inflate(R.layout.template_radio_button_segmented, null);

            // TODO(dxchen): Un-unscreamify once server work is done.

            radioButton.setText(prompt.getSelectChoiceText(choice));
            radioButton.setTag(i);
            radioButton.setId(QuestionWidget.newUniqueId());
            radioButton.setEnabled(!isReadOnly);
            radioButton.setFocusable(!isReadOnly);

            if (choice.getValue().equals(defaultAnswer)) {
                mGroup.check(i);
            }

            mGroup.addView(radioButton);
        }

        addView(mGroup);
    }

    @Override
    public SelectOneData getAnswer() {
        int checkedIndex = mGroup.getCheckedRadioButtonId();
        if (checkedIndex < 0) {
            return null;
        }

        View checkedRadioButton = mGroup.findViewById(checkedIndex);
        if (checkedRadioButton == null) {
            return null;
        }

        return new SelectOneData(
                new Selection(mChoices.get(mGroup.indexOfChild(checkedRadioButton))));
    }

    @Override
    public void clearAnswer() {
        mGroup.clearCheck();
    }

    @Override
    public void setFocus(Context context) {
        InputMethodManager inputManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }
}
