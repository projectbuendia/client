package org.odk.collect.android.widgets2.selectone;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.model.PrepopulatableFields;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets2.common.Appearance;
import org.odk.collect.android.widgets2.common.TypedWidget;

import java.util.List;

/**
 * A {@link SelectOneData} {@link TypedWidget} that displays a binary choice as a single button that
 * has an on and an off state.
 */
public class BinarySelectOneWidget extends TypedWidget<SelectOneData> {

    private static class OnRadioButtonClickListener implements OnClickListener {

        @Override
        public void onClick(View view) {
            // Unfocus any currently-focused view and hide the soft keyboard.
            View currentFocus = ((Activity) view.getContext()).getCurrentFocus();
            if (currentFocus != null) {
                currentFocus.clearFocus();
            }

            FormEntryActivity.hideKeyboard(view.getContext(), view);
        }
    }

    private static final OnClickListener ON_RADIO_BUTTON_CLICK_LISTENER =
            new OnRadioButtonClickListener();

    private SelectChoice mYesChoice;
    private SelectChoice mNoChoice;

    private CheckBox mCheckBox;

    public BinarySelectOneWidget(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        // TODO: Handle initial values.

        super(context, prompt, appearance, forceReadOnly);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        setYesNoChoices(prompt);

        mCheckBox =
                (CheckBox) inflater.inflate(R.layout.template_check_box_button, null);

        String defaultAnswer = prompt.getAnswerValue() == null
                ? null
                : ((Selection) prompt.getAnswerValue().getValue()).getValue();

        boolean isReadOnly = forceReadOnly || prompt.isReadOnly();

        mCheckBox.setText(prompt.getQuestionText());
        mCheckBox.setId(QuestionWidget.newUniqueId());
        mCheckBox.setEnabled(!isReadOnly);
        mCheckBox.setFocusable(!isReadOnly);
        mCheckBox.setOnClickListener(ON_RADIO_BUTTON_CLICK_LISTENER);

        if (mYesChoice.getValue().equals(defaultAnswer)) {
            mCheckBox.setChecked(true);
        }

        // Remove the views added by the base class.
        removeAllViews();

        addView(mCheckBox);
    }

    @Override
    public boolean forceSetAnswer(Object answer) {
        if (!(answer instanceof Integer)) {
            return false;
        }

        int typedAnswer = (Integer) answer;
        if (typedAnswer == PrepopulatableFields.YES) {
            mCheckBox.setChecked(true);
        }

        return false;
    }

    @Override
    public SelectOneData getAnswer() {
        return new SelectOneData(new Selection(mCheckBox.isChecked() ? mYesChoice : mNoChoice));
    }

    @Override
    public void clearAnswer() {
        mCheckBox.setChecked(false);
    }

    @Override
    public void setFocus(Context context) {
        InputMethodManager inputManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    private void setYesNoChoices(FormEntryPrompt prompt) {
        List<SelectChoice> choices = prompt.getSelectChoices();
        switch (choices.size()) {
            case 2:
                mYesChoice = choices.get(0);
                mNoChoice = choices.get(1);
                break;
            case 3:
                mYesChoice = choices.get(1);
                mNoChoice = choices.get(2);
                break;
            default:
                throw new IllegalArgumentException(
                        "The select choices in the current prompt must be either (in order): yes, "
                                + "no; or unknown, yes, and no.");
        }
    }
}
