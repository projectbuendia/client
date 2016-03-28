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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.OrderDeleteRequestedEvent;
import org.projectbuendia.client.events.actions.OrderSaveRequestedEvent;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.utils.Utils;

import java.util.Date;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/** A {@link DialogFragment} for adding a new user. */
public class OrderDialogFragment extends DialogFragment {

    /** For a duration < 3 days, provides strings expressing the duration in a friendly way. */
    private static final @StringRes int[] GIVE_FOR_DAYS_STATIC_STRINGS = new int[] {
            R.string.order_duration_unspecified,
            R.string.order_duration_stop_after_today,
            R.string.order_duration_stop_after_tomorrow
    };

    // TODO: Use a better result notification mechanism than EventBus for this.
    @Inject
    EventBus mEventBus;

    @InjectView(R.id.order_medication) EditText mMedication;
    @InjectView(R.id.order_dosage) EditText mDosage;
    @InjectView(R.id.order_frequency) EditText mFrequency;
    @InjectView(R.id.order_give_for_days) EditText mGiveForDays;
    @InjectView(R.id.order_start_date) TextView mStartDateView;
    @InjectView(R.id.order_start_date_change_button) View mStartDateChangeButton;
    @InjectView(R.id.order_give_for_days_label) TextView mGiveForDaysLabel;
    @InjectView(R.id.order_duration_label) TextView mDurationLabel;
    @InjectView(R.id.order_delete) Button mDelete;
    private LayoutInflater mInflater;
    private DateTime mStartDate;

    /** Creates a new instance and registers the given UI, if specified. */
    public static OrderDialogFragment newInstance(String patientUuid, Order order) {
        Bundle args = new Bundle();
        args.putString("patientUuid", patientUuid);
        args.putBoolean("new", order == null);

        // This time is used as the current time for any calculations in this dialog.
        // Use this value throughout instead of calling now().  This is necessary to maintain UI
        // consistency (e.g. if the dialog is opened before midnight and submitted after midnight).
        args.putLong("now_millis", DateTime.now().getMillis());

        if (order != null) {
            args.putString("uuid", order.uuid);
            args.putString("instructions", order.instructions);
            args.putLong("start_millis", order.start.getMillis());
            if (order.stop != null) {
                args.putLong("stop_millis", order.stop.getMillis());
            }
        }
        OrderDialogFragment fragment = new OrderDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
        App.getInstance().inject(this);
    }

    @Override public void onResume() {
        super.onResume();
        // Replace the existing button listener so we can control whether the dialog is dismissed.
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

    private void populateFields(Bundle args) {
        String instructions = args.getString("instructions");
        mMedication.setText(Order.getMedication(instructions));
        mDosage.setText(Order.getDosage(instructions));
        mFrequency.setText(Order.getFrequency(instructions));
        DateTime now = Utils.getDateTime(args, "now_millis");
        Long startMillis = Utils.getLong(args, "start_millis");
        mStartDate = (startMillis == null ? now : new DateTime(startMillis));
        Long stopMillis = Utils.getLong(args, "stop_millis");
        if (stopMillis != null) {
            LocalDate lastDay = new DateTime(stopMillis).toLocalDate();
            int days = Days.daysBetween(mStartDate.toLocalDate(), lastDay).getDays();
            // 1 day means stop after today, so we have to increment by 1.
            mGiveForDays.setText(String.format("%d", days + 1));
        }
        updateLabels();
    }

    private String formatDate(DateTime startDate) {
        // If the start date is the current date, return "Today"
        DateTime now = Utils.getDateTime(getArguments(), "now_millis");
        if (startDate.withTimeAtStartOfDay().equals(now.withTimeAtStartOfDay())) {
            return getResources().getString(R.string.today);
        }

        return getResources().getString(R.string.day_of_week_and_medium_date, startDate.toDate());
    }

    public void onSubmit(Dialog dialog) {
        String uuid = getArguments().getString("uuid");
        String patientUuid = getArguments().getString("patientUuid");
        String medication = mMedication.getText().toString().trim();
        String dosage = mDosage.getText().toString().trim();
        String frequency = mFrequency.getText().toString().trim();

        String instructions = Order.getInstructions(medication, dosage, frequency);
        String durationStr = mGiveForDays.getText().toString().trim();
        Integer durationDays = durationStr.isEmpty() ? null : Integer.valueOf(durationStr);
        boolean valid = true;
        if (medication.isEmpty()) {
            setError(mMedication, R.string.enter_medication);
            valid = false;
        }
        if (durationDays != null && durationDays == 0) {
            setError(mGiveForDays, R.string.order_give_for_days_cannot_be_zero);
            valid = false;
        }
        Utils.logUserAction("order_submitted",
            "valid", "" + valid,
            "uuid", uuid,
            "medication", medication,
            "dosage", dosage,
            "frequency", frequency,
            "instructions", instructions,
            "durationDays", "" + durationDays);

        if (!valid) {
            return;
        }

        dialog.dismiss();

        // Post an event that triggers the PatientChartController to save the order.
        mEventBus.post(new OrderSaveRequestedEvent(
                uuid, patientUuid, instructions, mStartDate, durationDays));
    }

    public void onDelete(Dialog dialog, final String orderUuid) {
        dialog.dismiss();

        new AlertDialog.Builder(getActivity())
            .setMessage(R.string.confirm_order_delete)
            .setTitle(R.string.title_confirmation)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int i) {
                    mEventBus.post(new OrderDeleteRequestedEvent(orderUuid));
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .create().show();
    }

    private void setError(EditText field, int resourceId) {
        field.setError(getResources().getString(resourceId));
        field.invalidate();
        field.requestFocus();
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        @SuppressLint("InflateParams") // It's a dialog, there's no root view.
        View fragment = mInflater.inflate(R.layout.order_dialog_fragment, null);
        ButterKnife.inject(this, fragment);
        mGiveForDays.addTextChangedListener(new DurationDaysWatcher());

        Bundle args = getArguments();
        boolean newOrder = args.getBoolean("new");
        String title = getString(newOrder ? R.string.title_new_order : R.string.title_edit_order);
        final String orderUuid = args.getString("uuid");
        populateFields(args);

        final Dialog dialog = new AlertDialog.Builder(getActivity())
            .setCancelable(false) // Disable auto-cancel.
            .setTitle(title)
            // The positive button uses dialog, so we have to set it below, after dialog is assigned.
            .setNegativeButton(R.string.cancel, null)
            .setView(fragment)
            .create();

        ((AlertDialog) dialog).setButton(Dialog.BUTTON_POSITIVE,
            getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialogInterface, int i) {
                    onSubmit(dialog);
                }
            });

        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                onDelete(dialog, orderUuid);
            }
        });

        mStartDateChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialogFragment dlg = DatePickerDialogFragment.create(mStartDate.toDate());
                dlg.setListener(mDateChosenListener);
                dlg.show(getFragmentManager(), "DatePicker");

            }
        });

        // Hide or show the "Delete" and "Change start date" buttons appropriately.
        Utils.showIf(mDelete, !newOrder);
        Utils.showIf(mStartDateChangeButton, newOrder);

        // Open the keyboard, ready to type into the medication field.
        dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mMedication.requestFocus();
        return dialog;
    }

    /** Updates the various labels in the form that react to changes in input fields. */
    void updateLabels() {
        // Start Date
        mStartDateView.setText(formatDate(mStartDate));

        // Duration
        String text = mGiveForDays.getText().toString().trim();
        int days = text.isEmpty() ? 0 : Integer.parseInt(text);
        LocalDate lastDay = mStartDate.toLocalDate().plusDays(days - 1);
        // TODO: use R.plurals instead.
        mGiveForDaysLabel.setText(
            days == 0 ? R.string.order_give_for_days :
                days == 1 ? R.string.order_give_for_day :
                    R.string.order_give_for_days);
        if (days < GIVE_FOR_DAYS_STATIC_STRINGS.length) {
            mDurationLabel.setText(GIVE_FOR_DAYS_STATIC_STRINGS[days]);
        } else {
            mDurationLabel.setText(getResources().getString(
                    R.string.order_duration_stop_after_date,
                    Utils.toShortString(lastDay)));
        }
    }

    class DurationDaysWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence c, int x, int y, int z) {
        }

        @Override public void onTextChanged(CharSequence c, int x, int y, int z) {
        }

        @Override public void afterTextChanged(Editable editable) {
            updateLabels();
        }
    }

    private final DatePickerDialogFragment.DateChosenListener mDateChosenListener =
            new DatePickerDialogFragment.DateChosenListener() {
        @Override
        public void onDateChosen(Date date) {
            mStartDate = new DateTime(date);
            updateLabels();
        }
    };

}
