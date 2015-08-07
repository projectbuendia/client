// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.projectbuendia.client.R;
import org.projectbuendia.client.events.OrderSaveRequestedEvent;
import org.projectbuendia.client.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/** A {@link DialogFragment} for adding a new user. */
public class OrderDialogFragment extends DialogFragment {
    /** Creates a new instance and registers the given UI, if specified. */
    public static OrderDialogFragment newInstance(
            String patientUuid, String previousOrderUuid) {
        Bundle args = new Bundle();
        args.putString("patientUuid", patientUuid);
        args.putString("previousOrderUuid", previousOrderUuid);
        OrderDialogFragment f = new OrderDialogFragment();
        f.setArguments(args);
        return f;
    }

    @InjectView(R.id.order_instructions) EditText mInstructions;
    @InjectView(R.id.order_stop_days) EditText mStopDays;

    private LayoutInflater mInflater;

    /** Called to save the new order when the user clicks OK. */
    public interface OrderSaver {
        void saveNewOrder(String patientUuid, String instructions, Integer stopDays);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        // Replace the existing button listener so we can prevent the dialog
        // from being dismissed when validation fails.
        final AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onSubmit(dialog);
                    }
                }
        );


    }


    public void onSubmit(AlertDialog dialog) {
        String instructions = mInstructions.getText().toString().trim();
        String stopDaysStr = mStopDays.getText().toString().trim();
        Integer stopDays = stopDaysStr.isEmpty() ? null : Integer.valueOf(stopDaysStr);
        boolean valid = true;
        if (instructions.isEmpty()) {
            setError(mInstructions, R.string.order_medication_cannot_be_blank);
            valid = false;
        }
        if (stopDays != null && stopDays == 0) {
            setError(mStopDays, R.string.order_stop_days_cannot_be_zero);
            valid = false;
        }
        Utils.logUserAction("order_submitted",
                "valid", "" + valid,
                "instructions", instructions,
                "stopDays", "" + stopDays);

        if (valid) {
            dialog.dismiss();

            // Post an event that triggers the PatientChartController to save the order.
            // TODO: Support revision of previousOrder in addition to creating new orders.
            EventBus.getDefault().post(new OrderSaveRequestedEvent(
                    getArguments().getString("previousOrderUuid"),
                    getArguments().getString("patientUuid"),
                    instructions, stopDays));
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.order_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        return new AlertDialog.Builder(getActivity())
                .setCancelable(false) // Disable auto-cancel.
                .setTitle(R.string.title_new_order)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(fragment)
                .create();
    }

    private void setError(EditText field, int resourceId) {
        field.setError(getResources().getString(resourceId));
        field.invalidate();
        field.requestFocus();
    }
}