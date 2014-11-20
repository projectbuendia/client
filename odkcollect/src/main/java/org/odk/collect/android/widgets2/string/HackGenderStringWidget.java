package org.odk.collect.android.widgets2.string;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.HorizontalScrollView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.widgets.QuestionWidget;
import org.odk.collect.android.widgets2.Appearance;
import org.odk.collect.android.widgets2.TypedWidget;

import java.util.List;

/**
 * A {@link StringData} {@link TypedWidget} for to show gender as buttons.
 */
public class HackGenderStringWidget extends TypedWidget<StringData> {

    private final String[] mChoices = new String[] { "Male", "Female" };
    private final RadioGroup mGroup;

    public HackGenderStringWidget(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        // TODO(dxchen): Handle initial values.

        super(context, prompt, appearance, forceReadOnly);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        HorizontalScrollView scrollView =
                (HorizontalScrollView) inflater.inflate(R.layout.template_segmented_group, null);
        mGroup = (RadioGroup) scrollView.findViewById(R.id.radio_group);

        boolean isReadOnly = forceReadOnly || prompt.isReadOnly();

        for (int i = 0; i < mChoices.length; i++) {
            String choice = mChoices[i];

            RadioButton radioButton =
                    (RadioButton) inflater.inflate(R.layout.template_radio_button_segmented, null);
            radioButton.setText(choice);
            radioButton.setTag(i);
            radioButton.setId(QuestionWidget.newUniqueId());
            radioButton.setEnabled(!isReadOnly);
            radioButton.setFocusable(!isReadOnly);

            mGroup.addView(radioButton);
        }

        addView(scrollView);
    }

    @Override
    public StringData getAnswer() {
        int checkedIndex = mGroup.getCheckedRadioButtonId();
        if (checkedIndex < 0) {
            return null;
        }

        View checkedRadioButton = mGroup.findViewById(checkedIndex);
        if (checkedRadioButton == null) {
            return null;
        }

        return new StringData(mChoices[mGroup.indexOfChild(checkedRadioButton)].substring(0, 1));
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

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {}
}
