package org.odk.collect.android.widgets2;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.widgets2.selectone.SelectOneWidgetFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * A factory that creates {@link TypedWidget}s.
 */
public class Widget2Factory {

    private static final String TAG = Widget2Factory.class.getName();

    private static final SparseArray<TypedWidgetFactory<? extends IAnswerData>> sFactoryRegistry;

    static {
        sFactoryRegistry = new SparseArray<TypedWidgetFactory<? extends IAnswerData>>();
        sFactoryRegistry.put(Constants.CONTROL_SELECT_ONE, new SelectOneWidgetFactory());
    }

    public static final Widget2Factory INSTANCE = new Widget2Factory();

    public TypedWidget<?> create(Context context, FormEntryPrompt prompt, boolean forceReadOnly) {
        TypedWidgetFactory<?> factory = sFactoryRegistry.get(prompt.getControlType());
        if (factory == null) {
            Log.e(
                    TAG,
                    "No TypedWidgetFactory registered for control type " + prompt.getControlType()
                            + ".");
            return null;
        }

        return factory.create(context, prompt, forceReadOnly);
    }

    private Widget2Factory() {}
}
