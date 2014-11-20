package org.odk.collect.android.widgets2;

import android.content.Context;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;

/**
 * A factory that creates {@link TypedWidget}s.
 */
public abstract class TypedWidgetFactory<T extends IAnswerData> {

    public abstract TypedWidget<T> create(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly);
}
