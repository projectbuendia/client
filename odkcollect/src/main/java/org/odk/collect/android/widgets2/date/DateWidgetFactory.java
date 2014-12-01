package org.odk.collect.android.widgets2.date;

import android.content.Context;

import org.javarosa.core.model.data.DateData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets2.common.Appearance;
import org.odk.collect.android.widgets2.common.TypedWidget;
import org.odk.collect.android.widgets2.common.TypedWidgetFactory;

/**
 * A {@link TypedWidgetFactory} that creates {@link DateData} widgets.
 */
public class DateWidgetFactory extends TypedWidgetFactory<DateData> {

    @Override
    public TypedWidget<DateData> create(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        if (appearance == null) {
            return null;
        }

        if ("minimal".equals(appearance.mPrimaryAppearance)) {
            return new TimeElapsedDateWidget(context, prompt, appearance, forceReadOnly);
        }

        return null;
    }
}
