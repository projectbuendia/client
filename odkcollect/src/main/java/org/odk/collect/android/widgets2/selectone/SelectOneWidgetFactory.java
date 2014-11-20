package org.odk.collect.android.widgets2.selectone;

import android.content.Context;

import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets2.Appearance;
import org.odk.collect.android.widgets2.TypedWidget;
import org.odk.collect.android.widgets2.TypedWidgetFactory;

/**
 * A {@link TypedWidgetFactory} that creates {@link SelectOneData} widgets.
 */
public class SelectOneWidgetFactory extends TypedWidgetFactory<SelectOneData> {

    public TypedWidget<SelectOneData> create(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly) {
        // TODO(dxchen):

//        if (appearance == null) {
//            return null;
//        }

//        if (appearance.mPrimaryAppearance.equals("minimal")
//                && appearance.hasQualifier("segmented")) {
            return new ButtonsSelectOneWidget(context, prompt, appearance, forceReadOnly);
//        }

//        return null;
    }
}
