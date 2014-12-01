package org.odk.collect.android.widgets2.group;

import android.content.Context;

import org.javarosa.form.api.FormEntryCaption;
import org.odk.collect.android.widgets2.common.Appearance;

/**
 * A factory that creates {@link WidgetGroupBuilder}s.
 */
public class WidgetGroupBuilderFactory {

    public WidgetGroupBuilder create(Context context, FormEntryCaption group) {
        Appearance appearance = Appearance.fromString(group.getAppearanceHint());

        if (appearance.hasQualifier("binary-select-one")) {
            return new BinarySelectOneWidgetGroupBuilder(appearance);
        }

        // TODO(dxchen): Remove this once the server sends back the right appearance.
        if (group.getQuestionText().startsWith("Symptoms")) {
            return new BinarySelectOneWidgetGroupBuilder(appearance);
        }

        return null;
    }
}
