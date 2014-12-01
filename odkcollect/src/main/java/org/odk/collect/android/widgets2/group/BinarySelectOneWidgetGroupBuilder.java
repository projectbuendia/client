package org.odk.collect.android.widgets2.group;

import android.content.Context;

import org.javarosa.form.api.FormEntryPrompt;

/**
 * A {@link WidgetGroupBuilder} that builds {@link BinarySelectOneWidgetGroup}s.
 */
public class BinarySelectOneWidgetGroupBuilder implements
        WidgetGroupBuilder<BinarySelectOneWidgetGroup, BinarySelectOneWidgetGroupBuilder> {

    @Override
    public BinarySelectOneWidgetGroupBuilder createAndAddWidget(
            Context context, FormEntryPrompt prompt, boolean forceReadOnly) {
        return null;
    }

    @Override
    public BinarySelectOneWidgetGroup build(Context context) {
        return null;
    }
}
