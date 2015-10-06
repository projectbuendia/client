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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;

import org.joda.time.LocalDate;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.OrderSaveRequestedEvent;
import org.projectbuendia.client.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/** A {@link DialogFragment} for adding a new user. */
public class OrderDialogFragment extends DialogFragment {
    @InjectView(R.id.order_medication) EditText mMedication;
    @InjectView(R.id.order_dosage) EditText mDosage;
    @InjectView(R.id.order_frequency) EditText mFrequency;
    @InjectView(R.id.order_give_for_days) EditText mGiveForDays;
    @InjectView(R.id.order_give_for_days_label) TextView mGiveForDaysLabel;
    @InjectView(R.id.order_duration_label) TextView mDurationLabel;
    private LayoutInflater mInflater;

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

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
    }

    @Override public void onResume() {
        super.onResume();
        // Replace the existing button listener so we can control whether the dialog is dismissed.
        final AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
            new View.OnClickListener() {
                @Override public void onClick(View view) {
                    onSubmit(dialog);
                }
            }
        );
    }

    public void onSubmit(AlertDialog dialog) {
        String medication = mMedication.getText().toString().trim();
        String dosage = mDosage.getText().toString().trim();
        String frequency = mFrequency.getText().toString().trim();

        String instructions = medication;
        if (!dosage.isEmpty()) {
            instructions += " " + dosage;
        }
        if (!frequency.isEmpty()) {
            instructions += " " + frequency + "x daily"; // TODO/i18n
        }
        String durationStr = mGiveForDays.getText().toString().trim();
        Integer durationDays = durationStr.isEmpty() ? null : Integer.valueOf(durationStr);
        boolean valid = true;
        if (medication.isEmpty()) {
            setError(mMedication, R.string.order_medication_cannot_be_blank);
            valid = false;
        }
        if (durationDays != null && durationDays == 0) {
            setError(mGiveForDays, R.string.order_give_for_days_cannot_be_zero);
            valid = false;
        }
        Utils.logUserAction("order_submitted",
            "valid", "" + valid,
            "medication", medication,
            "dosage", dosage,
            "frequency", frequency,
            "instructions", instructions,
            "durationDays", "" + durationDays);

        if (valid) {
            dialog.dismiss();

            // Post an event that triggers the PatientChartController to save the order.
            // TODO: Support revision of previousOrder in addition to creating new orders.
            EventBus.getDefault().post(new OrderSaveRequestedEvent(
                getArguments().getString("previousOrderUuid"),
                getArguments().getString("patientUuid"),
                instructions, durationDays));
        }
    }

    private void setError(EditText field, int resourceId) {
        field.setError(getResources().getString(resourceId));
        field.invalidate();
        field.requestFocus();
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.order_dialog_fragment, null);
        ButterKnife.inject(this, fragment);
        mGiveForDays.addTextChangedListener(new DurationDaysWatcher());
        mMedication.requestFocus();
        Dialog dialog = new AlertDialog.Builder(getActivity())
            .setCancelable(false) // Disable auto-cancel.
            .setTitle(R.string.title_new_order)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .setView(fragment)
            .create();
        // Open the keyboard, ready to type into the medication field.
        dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    class DurationDaysWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence c, int x, int y, int z) {
        }

        @Override public void onTextChanged(CharSequence c, int x, int y, int z) {
        }

        @Override public void afterTextChanged(Editable editable) {
            String text = mGiveForDays.getText().toString().trim();
            int days = text.isEmpty() ? 0 : Integer.parseInt(text);
            LocalDate lastDay = LocalDate.now().plusDays(days - 1);
            mGiveForDaysLabel.setText(
                days == 0 ? R.string.order_give_for_days :
                    days == 1 ? R.string.order_give_for_day :
                        R.string.order_give_for_days);
            mDurationLabel.setText(getResources().getString(
                days == 0 ? R.string.order_duration_unspecified :
                    days == 1 ? R.string.order_duration_stop_after_today :
                        days == 2 ? R.string.order_duration_stop_after_tomorrow :
                            R.string.order_duration_stop_after_date
            ).replace("%s", Utils.toShortString(lastDay)));
        }
    }
}
