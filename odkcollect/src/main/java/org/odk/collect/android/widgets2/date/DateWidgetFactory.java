package org.odk.collect.android.widgets2.date;

import android.content.Context;

import org.javarosa.core.model.data.DateData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets2.Appearance;
import org.odk.collect.android.widgets2.TypedWidget;
import org.odk.collect.android.widgets2.TypedWidgetFactory;

/**
 * A {@link TypedWidgetFactory} that creates {@link DateData} widgets.
 */
public class DateWidgetFactory extends TypedWidgetFactory<DateData> {

    @Override
    public TypedWidget<DateData> create(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        // TODO(dxchen): Actual logic plz!
        if (prompt.getQuestionText().toLowerCase().contains("birth")) {
            return new TimeElapsedDateWidget(
                    context,
                    prompt,
                    Appearance.fromString("minimal|show_years|show_months"),
                    forceReadOnly);
        }

        return null;
    }
}
