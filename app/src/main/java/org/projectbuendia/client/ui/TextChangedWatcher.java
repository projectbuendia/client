package org.projectbuendia.client.ui;

import android.text.Editable;
import android.text.TextWatcher;

public class TextChangedWatcher implements TextWatcher {
    private final Runnable callback;

    public TextChangedWatcher(Runnable callback) {
        this.callback = callback;
    }

    @Override public void beforeTextChanged(CharSequence c, int x, int y, int z) { }

    @Override public void onTextChanged(CharSequence c, int x, int y, int z) { }

    @Override public void afterTextChanged(Editable editable) {
        callback.run();
    }
}
