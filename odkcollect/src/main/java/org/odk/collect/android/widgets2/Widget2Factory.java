package org.odk.collect.android.widgets2;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets2.date.DateWidgetFactory;
import org.odk.collect.android.widgets2.selectone.SelectOneWidgetFactory;
import org.odk.collect.android.widgets2.string.StringWidgetFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory that creates {@link TypedWidget}s.
 */
public class Widget2Factory {

    private static final String TAG = Widget2Factory.class.getName();

    private static final SparseArray<SparseArray<TypedWidgetFactory<?>>> sFactoryRegistry;

    static {
        sFactoryRegistry = new SparseArray<SparseArray<TypedWidgetFactory<?>>>();
        register(Constants.CONTROL_SELECT_ONE, new SelectOneWidgetFactory());
        register(Constants.CONTROL_INPUT, Constants.DATATYPE_DATE, new DateWidgetFactory());
        register(Constants.CONTROL_INPUT, Constants.DATATYPE_TEXT, new StringWidgetFactory());
    }

    public static final Widget2Factory INSTANCE = new Widget2Factory();

    public TypedWidget<?> create(Context context, FormEntryPrompt prompt, boolean forceReadOnly) {
        SparseArray<TypedWidgetFactory<?>> dataTypeArray =
                sFactoryRegistry.get(prompt.getControlType());
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
        SparseArray<TypedWidgetFactory<?>> dataTypeArray = sFactoryRegistry.get(controlType);
        if (dataTypeArray == null) {
            dataTypeArray = new SparseArray<TypedWidgetFactory<?>>();
            sFactoryRegistry.put(controlType, dataTypeArray);
        }

        dataTypeArray.put(dataType, factory);
    }

    private Widget2Factory() {}
}
