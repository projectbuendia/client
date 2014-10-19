package org.msf.records.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Created by danieljulio on 19/10/2014.
 */

public class EditTextDialogFragment extends android.support.v4.app.DialogFragment {

    private static final String ITEM_LIST_KEY = "ITEM_LIST_KEY";
    String mHint;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Bundle bundle = getArguments();
        mHint = bundle.getString(ITEM_LIST_KEY);


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        EditText editText;


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(100, 25, 100, 25);

        for (int i = 0; i < 3; i++) {

            editText = new EditText(getActivity());
            editText.setLayoutParams(params);
            editText.setHint("" + i);
            linearLayout.addView(editText);
        }

        return linearLayout;
    }
}
