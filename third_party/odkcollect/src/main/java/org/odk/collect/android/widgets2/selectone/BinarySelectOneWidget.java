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
import org.odk.collect.android.model.Preset;
import org.odk.collect.android.utilities.Utils;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets2.common.Appearance;
import org.odk.collect.android.widgets2.common.TypedWidget;

import java.util.ArrayList;
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

        mCheckBox.setText(Utils.localize(prompt.getQuestionText(), context));
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
        if (typedAnswer == Preset.YES) {
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

    /** Tries to determine whether a choice is the "yes" answer. */
    private boolean isYesChoice(SelectChoice choice) {
        return choice.getValue().startsWith("1065^") ||
                choice.getLabelInnerText().trim().toLowerCase().startsWith("yes");
    }

    private void setYesNoChoices(FormEntryPrompt prompt) {
        List<SelectChoice> choices = prompt.getSelectChoices();
        if (choices.size() != 2) {
            throw new IllegalArgumentException(String.format(
                    "Need exactly two choices but question \"%s\" has %d",
                    mPrompt.getLongText(), choices.size()));
        }

        // Find the "yes" answer and ensure it is unique.
        List<Integer> yesChoices = new ArrayList<>();
        for (int i = 0; i < choices.size(); i++) {
            if (isYesChoice(choices.get(i))) {
                yesChoices.add(i);
            }
        }
        if (yesChoices.size() != 1) {
            throw new IllegalArgumentException(String.format(
                    "Did not find a unique \"yes\" choice for question \"%s\"",
                    mPrompt.getLongText()));
        }

        int yesIndex = yesChoices.get(0);
        mYesChoice = choices.get(yesIndex);
        mNoChoice = choices.get(1 - yesIndex);
    }
}
