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
            return new BinarySelectTableBuilder(appearance);
        }

        return null;
    }
}
