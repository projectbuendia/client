package org.msf.records.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by danieljulio on 19/10/2014.
 */

public class EditTextDialogFragment extends android.support.v4.app.DialogFragment {

    private static final String ITEM_LIST_KEY = "ITEM_LIST_KEY";
    public static final String GRID_ITEM_DONE_LISTENER = "GRID_ITEM_DONE_LISTENER";
    OnItemClickListener mPositiveClickListener;
    String[] mArray;

    public static abstract class OnItemClickListener implements Serializable {
        public abstract void onPositiveButtonClick(String[] data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Bundle bundle = getArguments();
        mArray = bundle.getStringArray(ITEM_LIST_KEY);

        mPositiveClickListener = (OnItemClickListener) bundle.getSerializable(GRID_ITEM_DONE_LISTENER);



    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        getDialog().setTitle("Edit Patient Info");
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        EditText editText;
        final List<EditText> allEditTexts = new ArrayList<EditText>();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(100, 50, 100, 50);

        for (int i = 0; i < mArray.length; i++) {
            editText = new EditText(getActivity());
            allEditTexts.add(editText);
            editText.setLayoutParams(params);
            editText.setHint(mArray[i]);
            linearLayout.addView(editText);
        }

        LinearLayout buttonBar = new LinearLayout(getActivity(), null, android.R.attr.buttonBarStyle);
        Button negativeButton = new Button(getActivity(), null, android.R.attr.buttonBarButtonStyle);
        Button positiveButton = new Button(getActivity(), null, android.R.attr.buttonBarButtonStyle);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        positiveButton.setLayoutParams(param);
        negativeButton.setLayoutParams(param);
        positiveButton.setText("Save");
        negativeButton.setText("Cancel");
        buttonBar.addView(negativeButton);
        buttonBar.addView(positiveButton);
        linearLayout.addView(buttonBar);

        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] data = new String[mArray.length];
                for (int i = 0; i < data.length; i++) {
                    data[i] = allEditTexts.get(i).getText().toString();
                }
                mPositiveClickListener.onPositiveButtonClick(data);
                getDialog().dismiss();
            }
        });

        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });


        return linearLayout;
    }
}
