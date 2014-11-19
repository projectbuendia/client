package org.odk.collect.android.widgets2.selectone;

import android.content.Context;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets2.Appearance;
import org.odk.collect.android.widgets2.TypedWidget;
import org.odk.collect.android.widgets2.TypedWidgetFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * A {@link org.odk.collect.android.widgets2.TypedWidgetFactory} that creates {@link SelectOneData}
 * widgets.
 */
public class SelectOneWidgetFactory extends TypedWidgetFactory<SelectOneData> {

    @Override
    public TypedWidget<SelectOneData> create(
            Context context, FormEntryPrompt prompt, boolean forceReadOnly) {
        Appearance appearance = Appearance.fromString(prompt.getAppearanceHint());
//        if (appearance == null) {
//            return null;
//        }

//        if (appearance.mPrimaryAppearance.equals("minimal")
//                && appearance.hasQualifier("segmented")) {
            return new SegmentedRadioWidget(context, prompt, forceReadOnly);
//        }

//        return null;
    }
}
