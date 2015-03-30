package org.odk.collect.android.widgets2.group;

import android.view.ViewManager;
import android.view.ViewParent;

import org.odk.collect.android.widgets2.common.TypedWidget;

import java.util.List;

/**
 * A group of {@link org.odk.collect.android.widgets2.common.TypedWidget}s that should be displayed
 * together.
 *
 * <p>Classes that implement {@link WidgetGroup} must also extend {@link android.view.ViewGroup},
 * though this is only loosely enforced by {@link WidgetGroup} extending {@link ViewParent} and
 * {@link ViewManager}.
 */
public interface WidgetGroup extends ViewParent, ViewManager {

    List<TypedWidget<?>> getWidgets();
}
