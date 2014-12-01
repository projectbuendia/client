package org.odk.collect.android.widgets2.group;

import android.content.Context;

import org.javarosa.form.api.FormEntryPrompt;

/**
 * A builder that builds widget groups.
 */
public interface WidgetGroupBuilder<T extends WidgetGroup, U extends WidgetGroupBuilder<T, U>> {

    /**
     * Creates and adds a widget for the given prompt to the widget group.
     *
     * @throws IllegalArgumentException if the current prompt cannot be created in the current
     *                                  widget group
     */
    U createAndAddWidget(Context context, FormEntryPrompt prompt, boolean forceReadOnly);

    /**
     * Builds the widget group.
     */
    T build(Context context);
}
