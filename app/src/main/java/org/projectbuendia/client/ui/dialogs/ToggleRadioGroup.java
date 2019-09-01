package org.projectbuendia.client.ui.dialogs;

import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.HashMap;
import java.util.Map;

import static org.projectbuendia.client.utils.Utils.eq;

public class ToggleRadioGroup<T> {
    private final RadioGroup group;
    private Map<T, RadioButton> buttons = new HashMap<>();
    private T selection = null;

    public ToggleRadioGroup(RadioGroup group) {
        this.group = group;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            T value;
            try {
                value = (T) child.getTag();
            } catch (ClassCastException e) {
                continue;
            }
            if (child instanceof RadioButton && value != null) {
                buttons.put(value, (RadioButton) child);
                child.setOnClickListener(view -> setSelection(
                    eq(selection, value) ? null : value));
            }
        }
    }

    public void setSelection(T value) {
        if (value == null) {
            group.clearCheck();
        } else {
            RadioButton button = buttons.get(value);
            if (button != null) button.setChecked(true);
        }
        selection = value;
    }

    public T getSelection() {
        return selection;
    }
}
