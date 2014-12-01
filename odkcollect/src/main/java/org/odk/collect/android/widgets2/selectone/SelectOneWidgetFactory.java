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

        // TODO(dxchen): This is for testing!
        if (prompt.getSelectChoices().size() == 3) {
            return new BinarySelectOneWidget(context, prompt, appearance, forceReadOnly);
        }

        // TODO(dxchen): Uncomment this when ready!
//
//        if (appearance == null) {
//            return null;
//        }

//        if (appearance.mPrimaryAppearance.equals("minimal")
//                && appearance.hasQualifier("buttons")) {
            return new ButtonsSelectOneWidget(context, prompt, appearance, forceReadOnly);
//        }
//
//        return null;
    }
}
