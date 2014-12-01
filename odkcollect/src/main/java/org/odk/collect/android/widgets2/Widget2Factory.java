package org.odk.collect.android.widgets2;

import android.content.Context;
import android.util.SparseArray;

import org.javarosa.core.model.Constants;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets2.date.DateWidgetFactory;
import org.odk.collect.android.widgets2.group.WidgetGroupBuilder;
import org.odk.collect.android.widgets2.group.WidgetGroupBuilderFactory;
import org.odk.collect.android.widgets2.selectone.SelectOneWidgetFactory;
import org.odk.collect.android.widgets2.string.StringWidgetFactory;

/**
 * A factory that creates {@link TypedWidget}s.
 */
public class Widget2Factory {

    private static final String TAG = Widget2Factory.class.getName();

    private static final WidgetGroupBuilderFactory sWidgetGroupFactory;
    private static final SparseArray<SparseArray<TypedWidgetFactory<?>>> sWidgetFactoryRegistry;

    static {
        sWidgetGroupFactory = new WidgetGroupBuilderFactory();
        sWidgetFactoryRegistry = new SparseArray<SparseArray<TypedWidgetFactory<?>>>();
        register(Constants.CONTROL_SELECT_ONE, new SelectOneWidgetFactory());
        register(Constants.CONTROL_INPUT, Constants.DATATYPE_DATE, new DateWidgetFactory());
        register(Constants.CONTROL_INPUT, Constants.DATATYPE_TEXT, new StringWidgetFactory());
    }

    public static final Widget2Factory INSTANCE = new Widget2Factory();

    /**
     * Creates a {@link WidgetGroupBuilder} for a given group or returns {@code null} if unable to
     * create a builder.
     */
    public WidgetGroupBuilder createGroup(
            Context context, FormEntryPrompt prompt, boolean forceReadOnly) {
        return sWidgetGroupFactory.create(context, prompt, forceReadOnly);
    }

    /**
     * Creates a widget for a given field or returns {@code null} if unable to create a widget for
     * that field.
     */
    public TypedWidget<?> create(
            Context context, FormEntryPrompt prompt, boolean forceReadOnly) {
        SparseArray<TypedWidgetFactory<?>> dataTypeArray =
                sWidgetFactoryRegistry.get(prompt.getControlType());
        if (dataTypeArray == null) {
//            Log.e(
//                    TAG,
//                    "No TypedWidgetFactory registered for control type "
//                            + prompt.getControlType() + ".");
            return null;
        }
        TypedWidgetFactory<?> factory = dataTypeArray.get(prompt.getDataType());
        if (factory == null) {
            factory = dataTypeArray.get(-1);
            if (factory == null) {
//                Log.e(
//                        TAG,
//                        "No TypedWidgetFactory registered for control type "
//                                + prompt.getControlType() + " and data type "
//                                + prompt.getDataType() + " (and no default TypedWidgetFactory for "
//                                + "that control type is registered).");
                return null;
            }
        }
        if (factory == null) {
            return null;
        }

        return factory.create(
                context, prompt, Appearance.fromString(prompt.getAppearanceHint()), forceReadOnly);
    }

    private static void register(int controlType, TypedWidgetFactory<?> factory) {
        register(controlType, -1, factory);
    }

    private static void register(int controlType, int dataType, TypedWidgetFactory<?> factory) {
        SparseArray<TypedWidgetFactory<?>> dataTypeArray = sWidgetFactoryRegistry.get(controlType);
        if (dataTypeArray == null) {
            dataTypeArray = new SparseArray<TypedWidgetFactory<?>>();
            sWidgetFactoryRegistry.put(controlType, dataTypeArray);
        }

        dataTypeArray.put(dataType, factory);
    }

    private Widget2Factory() {}
}
