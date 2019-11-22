package org.odk.collect.android.widgets2.selectone;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Build;
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
 * A {@link SelectOneData} {@link TypedWidget} that displays a binary choice
 * as a single button with a "yes" state, a "no" state, and an "unanswered" state.
 * Tapping the button toggles the state between "yes" and "unanswered"; the
 * setState() method allows setting the state to "yes", "unanswered", or "no".
 */
public class BinarySelectOneWidget extends TypedWidget<SelectOneData> implements View.OnClickListener {

    private SelectChoice mYesChoice;
    private SelectChoice mNoChoice;
    private CheckBox mCheckBox;
    private OnClickListener mOnClickCallback = null;
    private Boolean mState = null;  // true, false, or null (null means "unanswered")

    public BinarySelectOneWidget(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        super(context, prompt, appearance, forceReadOnly);

        mCheckBox = (CheckBox) LayoutInflater.from(getContext()).inflate(
            R.layout.template_check_box_button, null);
        mCheckBox.setText(Utils.localize(prompt.getQuestionText(), context));
        mCheckBox.setId(QuestionWidget.newUniqueId());
        boolean isReadOnly = forceReadOnly || prompt.isReadOnly();
        mCheckBox.setEnabled(!isReadOnly);
        mCheckBox.setFocusable(!isReadOnly);
        mCheckBox.setOnClickListener(this);

        findYesNoChoices(prompt);
        String defaultAnswer = prompt.getAnswerValue() == null ? null
            : ((Selection) prompt.getAnswerValue().getValue()).getValue();
        setState(Utils.eq(defaultAnswer, mYesChoice.getValue()) ? true : null);

        removeAllViews();  // remove views added by the base class
        addView(mCheckBox);
    }

    public void setOnClickCallback(OnClickListener callback) {
        mOnClickCallback = callback;
    }

    @Override public void onClick(View view) {
        clearActivityFocus((Activity) view.getContext());
        FormEntryActivity.hideKeyboard(view.getContext(), view);

        if (mOnClickCallback != null) {
            mOnClickCallback.onClick(this);
        }
    }

    public Boolean getState() {
        return mCheckBox.isChecked();
    }

    public void setState(Boolean state) {
        mState = state;
        mCheckBox.setChecked(mState == Boolean.TRUE);
        mCheckBox.setTextColor(mState == Boolean.FALSE ? 0xffc80080 : 0xff000000);

        if (Build.VERSION.SDK_INT >= 21) {
            mCheckBox.setButtonTintMode(PorterDuff.Mode.SCREEN);
            mCheckBox.setButtonTintList(
                mState == Boolean.FALSE ?
                    new ColorStateList(new int[0][0], new int[] {0x80ff0000}) : null
            );
        }
    }

    @Override public boolean forceSetAnswer(Object answer) {
        if (answer instanceof Integer) {
            int preset = (Integer) answer;
            setState(preset == Preset.YES ? true : preset == Preset.NO ? false : null);
        }
        return false;
    }

    @Override public SelectOneData getAnswer() {
        return new SelectOneData(
            mState == Boolean.TRUE ? new Selection(mYesChoice) :
                mState == Boolean.FALSE ? new Selection(mNoChoice) : new Selection());
    }

    @Override public void clearAnswer() {
        setState(null);
    }

    @Override public void setFocus(Context context) {
        InputMethodManager inputManager = (InputMethodManager) context
            .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    public void clearActivityFocus(Activity activity) {
        View currentFocus = activity.getCurrentFocus();
        if (currentFocus != null) {
            currentFocus.clearFocus();
        }
    }

    private void findYesNoChoices(FormEntryPrompt prompt) {
        List<SelectChoice> choices = prompt.getSelectChoices();
        if (choices.size() != 2) {
            throw new IllegalArgumentException(String.format(
                "Question \"%s\" has %d choices, but expected 2",
                mPrompt.getLongText(), choices.size()));
        }

        List<Integer> yesChoices = new ArrayList<>();
        for (int i = 0; i < choices.size(); i++) {
            if (isYesChoice(choices.get(i))) {
                yesChoices.add(i);
            }
        }
        if (yesChoices.size() != 1) {
            throw new IllegalArgumentException(String.format(
                "Question \"%s\" has %d \"yes\" choices, but expected 1",
                mPrompt.getLongText(), yesChoices.size()));
        }

        int yesIndex = yesChoices.get(0);
        mYesChoice = choices.get(yesIndex);
        mNoChoice = choices.get(1 - yesIndex);
    }

    /** Guesses whether a choice represents "yes". */
    private boolean isYesChoice(SelectChoice choice) {
        return choice.getValue().startsWith("1065^") ||
            choice.getLabelInnerText().trim().toLowerCase().startsWith("yes");
    }
}
