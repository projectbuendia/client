package org.odk.collect.android.widgets2.string;

import android.content.Context;

import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets2.Appearance;
import org.odk.collect.android.widgets2.TypedWidget;
import org.odk.collect.android.widgets2.TypedWidgetFactory;

/**
 * A {@link TypedWidgetFactory} that creates {@link StringData} widgets.
 */
public class StringWidgetFactory extends TypedWidgetFactory<StringData> {

    public TypedWidget<StringData> create(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {

        // Currently, there are no typed string widgets.

        return null;
    }
}
