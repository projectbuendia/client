package org.odk.collect.android.widgets2.group;

import android.content.Context;
import android.view.ViewGroup;

import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.model.Preset;
import org.odk.collect.android.widgets2.common.Appearance;

/**
 * A builder that builds widget groups.
 *
 * @param <T> the type of {@link WidgetGroup} being built
 * @param <U> a self-referential type parameter
 */
public interface WidgetGroupBuilder<
        T extends ViewGroup & WidgetGroup,
        U extends WidgetGroupBuilder<T, U>> {

    // TODO: Consider adding an onError callback to be invoked on add error.

    /**
     * Creates and adds a widget for the given prompt to the widget group.
     *
     * <p>If the widget cannot be added, a message will be logged but execution will continue.
     */
    U createAndAddWidget(
            Context context,
            FormEntryPrompt prompt,
            Appearance appearance,
            boolean forceReadOnly,
            int id,
            Preset fields);

    /**
     * Builds the widget group.
     */
    T build(Context context);
}
