package org.odk.collect.android.widgets2.group;

import android.content.Context;

import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets2.common.Appearance;

/**
 * A factory that creates {@link WidgetGroupBuilder}s.
 */
public class WidgetGroupBuilderFactory {

    public static WidgetGroupBuilder create(
            Context context, FormEntryPrompt prompt, boolean forceReadOnly) {
        Appearance appearance = Appearance.fromString(prompt.getAppearanceHint());

        if (appearance.hasQualifier("binary-select-one")) {
            return new BinarySelectOneWidgetGroupBuilder(appearance);
        }

        return null;
    }
}
