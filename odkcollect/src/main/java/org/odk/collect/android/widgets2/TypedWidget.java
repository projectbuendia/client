package org.odk.collect.android.widgets2;

import android.content.Context;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets.QuestionWidget;

/**
 * A {@link QuestionWidget} with a typed answer.
 */
public abstract class TypedWidget<T extends IAnswerData> extends QuestionWidget {

    protected final Appearance mAppearance;

    public TypedWidget(Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        super(context, prompt);

        mAppearance = appearance;
    }

    @Override
    public abstract T getAnswer();
}
