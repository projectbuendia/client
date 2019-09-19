package org.projectbuendia.client.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class EditTextWatcher implements TextWatcher {
    private Runnable callback = null;
    private boolean inCallback = false;
    private final EditText[] fields;

    public EditTextWatcher(EditText... fields) {
        this.fields = fields;
    }

    public void onChange(Runnable callback) {
        this.callback = callback;
        for (EditText field : fields) {
            field.addTextChangedListener(this);
        }
    }

    @Override public void beforeTextChanged(CharSequence c, int x, int y, int z) { }

    @Override public void onTextChanged(CharSequence c, int x, int y, int z) { }

    @Override public void afterTextChanged(Editable editable) {
        if (callback != null && !inCallback) {
            inCallback = true;
            try {
                callback.run();
            } finally {
                inCallback = false;
            }
        }
    }
}
