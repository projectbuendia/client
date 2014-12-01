package org.odk.collect.android.widgets2.selectone;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.utilities.StringUtils;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets2.common.Appearance;
import org.odk.collect.android.widgets2.common.TypedWidget;

import java.util.List;

/**
 * A {@link SelectOneData} {@link TypedWidget} that displays a binary choice as a single button that
 * has an on and an off state.
 */
public class BinarySelectOneWidget extends TypedWidget<SelectOneData> {

    private SelectChoice mYesChoice;
    private SelectChoice mNoChoice;

    private CheckBox mCheckBox;

    public BinarySelectOneWidget(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        // TODO(dxchen): Handle initial values.

        super(context, prompt, appearance, forceReadOnly);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        setYesNoChoices(prompt);

        mCheckBox =
                (CheckBox) inflater.inflate(R.layout.template_check_box_button, null);

        String defaultAnswer = prompt.getAnswerValue() == null
                ? null
                : ((Selection) prompt.getAnswerValue().getValue()).getValue();

        boolean isReadOnly = forceReadOnly || prompt.isReadOnly();

        mCheckBox.setText(StringUtils.unscreamify(prompt.getQuestionText()));
        mCheckBox.setId(QuestionWidget.newUniqueId());
        mCheckBox.setEnabled(!isReadOnly);
        mCheckBox.setFocusable(!isReadOnly);

        if (mYesChoice.getValue().equals(defaultAnswer)) {
            mCheckBox.setChecked(true);
        }

        addView(mCheckBox);
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
}
