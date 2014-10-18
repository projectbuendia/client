package org.msf.records.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by danieljulio on 06/10/2014.
 */
public class DialogMultipleChoiceList {

    private static final String TAG = ListDialogFragment.class.getName();

    public Dialog onCreateDialog(Bundle bundle, Context context) {

        String [] valueArray = bundle.getStringArray("key");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Patient Status")
                .setMultiChoiceItems(valueArray, null, new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                    }
                });

        return builder.show();
    }

}

