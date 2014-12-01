package org.odk.collect.android.widgets2.group;

import android.content.Context;

import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets2.common.Appearance;

/**
 * A builder that builds widget groups.
 */
public interface WidgetGroupBuilder<T extends WidgetGroup, U extends WidgetGroupBuilder<T, U>> {

    // TODO(dxchen): Consider adding an onError callback to be invoked on add error.

    /**
     * Creates and adds a widget for the given prompt to the widget group.
     *
     * <p>If the widget cannot be added, a message will be logged but execution will continue.
     */
    U createAndAddWidget(
            Context context, FormEntryPrompt prompt, Appearance appearance, boolean forceReadOnly);

    /**
     * Builds the widget group.
     */
    T build(Context context);
}
