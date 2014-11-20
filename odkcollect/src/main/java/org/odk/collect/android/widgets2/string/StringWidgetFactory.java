package org.odk.collect.android.widgets2.string;

import android.content.Context;

import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets2.Appearance;
import org.odk.collect.android.widgets2.TypedWidget;
import org.odk.collect.android.widgets2.TypedWidgetFactory;
import org.odk.collect.android.widgets2.selectone.ButtonsSelectOneWidget;

/**
 * A {@link TypedWidgetFactory} that creates {@link StringData} widgets.
 */
public class StringWidgetFactory extends TypedWidgetFactory<StringData> {

    public TypedWidget<StringData> create(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        // TODO(dxchen): HACK! This exists only to special-case the gender widget, which is a string
        // widget on the server.

        if (prompt.getQuestionText().toLowerCase().contains("gender")) {
            return new HackGenderStringWidget(context, prompt, appearance, forceReadOnly);
        }

        return null;
    }
}
