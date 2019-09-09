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
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.OrderDeleteRequestedEvent;
import org.projectbuendia.client.events.actions.OrderAddRequestedEvent;
import org.projectbuendia.client.events.actions.OrderStopRequestedEvent;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.ui.AutocompleteAdapter;
import org.projectbuendia.client.ui.MedCompleter;
import org.projectbuendia.client.ui.TextChangedWatcher;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.Utils;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.utils.Utils.DateStyle.SENTENCE_MONTH_DAY;

/** A {@link DialogFragment} for adding a new user. */
public class OrderDialogFragment extends DialogFragment {
    public static final int MAX_FREQUENCY = 24;  // maximum 24 times per day
    public static final int MAX_DURATION_DAYS = 30;  // maximum 30 days
    public static final String[] ROUTE_LABELS = {
        "PO", "IV", "IM", "SC"
    };
    // This should match the left/right padding in the TextViews in captioned_item.xml.
    private static final int ITEM_HORIZONTAL_PADDING = 12;

    @InjectView(R.id.order_medication) AutoCompleteTextView mMedication;
    @InjectView(R.id.order_route) EditText mRoute;
    @InjectView(R.id.order_dosage) EditText mDosage;
    @InjectView(R.id.order_frequency) EditText mFrequency;
    @InjectView(R.id.order_times_per_day_label) TextView mTimesPerDayLabel;
    @InjectView(R.id.order_give_for_days) EditText mGiveForDays;
    @InjectView(R.id.order_give_for_days_label) TextView mGiveForDaysLabel;
    @InjectView(R.id.order_duration_label) TextView mDurationLabel;
    @InjectView(R.id.order_notes) EditText mNotes;
    @InjectView(R.id.order_stop_now) Button mStopNow;
    @InjectView(R.id.order_delete) Button mDelete;

    private ContextUtils u;
    private String mOrderUuid;

    /** Creates a new instance and registers the given UI, if specified. */
    public static OrderDialogFragment newInstance(
        String patientUuid, Order order, List<DateTime> executionTimes) {
        // This time is used as the current time for any calculations in this dialog.
        // Use this value throughout instead of calling now().  This is necessary to maintain UI
        // consistency (e.g. if the dialog is opened before midnight and submitted after midnight).
        DateTime now = DateTime.now();

        Bundle args = new Bundle();
        args.putString("patientUuid", patientUuid);
        args.putBoolean("new", order == null);
        args.putBoolean("executed", Utils.hasItems(executionTimes));
        args.putLong("now_millis", now.getMillis());

        if (order != null) {
            args.putString("uuid", order.uuid);
            args.putString("medication", order.instructions.medication);
            args.putString("route", order.instructions.route);
            args.putString("dosage", order.instructions.dosage);
            args.putInt("frequency", order.instructions.frequency);
            args.putString("notes", order.instructions.notes);
            Utils.putDateTime(args, "start_millis", order.start);
            Utils.putDateTime(args, "stop_millis", order.stop);
            args.putBoolean("stopped", !order.isSeries() ||
                (order.stop != null && now.isAfter(order.stop)));
        }
        OrderDialogFragment fragment = new OrderDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        u = ContextUtils.from(getActivity());
    }

    @Override public void onResume() {
        super.onResume();

        // Open the keyboard, ready to type into the medication field.
        mMedication.requestFocus();

        // After the dialog has been laid out and positioned, we can figure out
        // how to position and size the autocompletion dropdown.
        mMedication.getViewTreeObserver().addOnPreDrawListener(() -> {
            adjustDropDownSize(mMedication, ITEM_HORIZONTAL_PADDING);
            return true;
        });
    }

    private void populateFields(Bundle args) {
        mMedication.setText(args.getString("medication"));
        mRoute.setText(args.getString("route"));
        mDosage.setText(args.getString("dosage"));
        int frequency = args.getInt("frequency");
        mFrequency.setText(frequency > 0 ? Integer.toString(frequency) : "");
        mNotes.setText(args.getString("notes"));
        DateTime start = Utils.getDateTime(args, "start_millis");
        DateTime stop = Utils.getDateTime(args, "stop_millis");
        if (stop != null) {
            int days = Days.daysBetween(start.toLocalDate(), stop.toLocalDate()).getDays();
            if (days > 0) {
                mGiveForDays.setText("" + days);
            }
        }
        updateLabels();
    }

    private void addListeners() {
        // TODO(ping): Replace the mRoute EditText with a properly styled Spinner.
        mRoute.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) v.callOnClick();
        });
        mRoute.setOnClickListener(v -> {
            int index = Arrays.asList(ROUTE_LABELS).indexOf(mRoute.getText().toString());
            new AlertDialog.Builder(getActivity())
                .setTitle(R.string.route_of_administration)
                .setSingleChoiceItems(ROUTE_LABELS, index, (dialog, which) -> {
                    mRoute.setText(ROUTE_LABELS[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        });

        mFrequency.addTextChangedListener(new TextChangedWatcher(this::updateLabels));
        mGiveForDays.addTextChangedListener(new TextChangedWatcher(this::updateLabels));

        mStopNow.setOnClickListener(view -> onStopNow(getDialog(), mOrderUuid));
        mDelete.setOnClickListener(view -> onDelete(getDialog(), mOrderUuid));
    }

    /** Adjusts the size of the autocomplete dropdown according to other UI elements. */
    private void adjustDropDownSize(AutoCompleteTextView textView, int itemHorizontalPadding) {
        // Get the visible area of the activity, excluding the soft keyboard.
        View activityRoot = getActivity().getWindow().getDecorView().getRootView();
        Rect visibleFrame = new Rect();
        activityRoot.getWindowVisibleDisplayFrame(visibleFrame);

        // Find the bottom of the text field, where the dropdown list is attached.
        int[] textViewLocation = new int[2];
        textView.getLocationOnScreen(textViewLocation);
        int textViewTop = textViewLocation[1];
        int textViewBottom = textViewTop + textView.getHeight();

        // Limit the height of the autocomplete dropdown list to stay within the
        // visible area of the activity; otherwise, the list will extend to the
        // bottom of the screen, where it is covered up by the soft keyboard.
        textView.setDropDownHeight(visibleFrame.bottom - textViewBottom);

        // Expand the dropdown window left and right to accommodate left/right
        // padding inside dropdown list items, so that the text in the list items
        // is horizontally aligned with the text in the input field.
        textView.setDropDownHorizontalOffset(-itemHorizontalPadding);
        textView.setDropDownWidth(mMedication.getWidth() + itemHorizontalPadding * 2);
    }

    public void onSubmit(DialogInterface dialog) {
        String uuid = getArguments().getString("uuid");
        String patientUuid = getArguments().getString("patientUuid");
        DateTime start = Utils.getDateTime(getArguments(), "start_millis");
        DateTime stop = Utils.getDateTime(getArguments(), "stop_millis");
        DateTime now = Utils.getDateTime(getArguments(), "now_millis");
        boolean newOrder = getArguments().getBoolean("new");
        boolean stopped = getArguments().getBoolean("stopped");
        boolean executed = getArguments().getBoolean("executed");
        String medication = mMedication.getText().toString().trim();
        String route = mRoute.getText().toString().trim();
        String dosage = mDosage.getText().toString().trim();
        int frequency = Utils.toIntOrDefault(mFrequency.getText().toString().trim(), 0);
        String notes = mNotes.getText().toString().trim();
        Order.Instructions instructions = new Order.Instructions(medication, route, dosage, frequency, notes);

        String durationStr = mGiveForDays.getText().toString().trim();
        Integer durationDays = durationStr.isEmpty() ? null : Integer.valueOf(durationStr);
        boolean valid = true;
        if (medication.isEmpty()) {
            setError(mMedication, R.string.enter_medication);
            valid = false;
        }
        if (frequency > MAX_FREQUENCY) {
            setError(mFrequency, R.string.order_cannot_exceed_n_times_per_day, MAX_FREQUENCY);
            valid = false;
        }
        if (durationDays != null) {
            if (durationDays == 0) {
                setError(mGiveForDays, R.string.order_give_for_days_cannot_be_zero);
                valid = false;
            }
            if (durationDays > MAX_DURATION_DAYS) {
                setError(mGiveForDays, R.string.order_cannot_exceed_n_days, MAX_DURATION_DAYS);
                valid = false;
            }
            if (start != null && start.plusDays(durationDays).isBefore(now)) {
                setError(mGiveForDays, R.string.order_cannot_stop_in_past);
                valid = false;
            }
        }
        Utils.logUserAction("order_submitted",
            "valid", "" + valid,
            "uuid", uuid,
            "medication", medication,
            "route", route,
            "dosage", dosage,
            "frequency", "" + frequency,
            "notes", notes,
            "durationDays", "" + durationDays);
        if (!valid) {
            return;
        }

        dialog.dismiss();

        start = Utils.orDefault(start, now);

        // Post an event that triggers the PatientChartController to save the order.
        EventBus.getDefault().post(new OrderAddRequestedEvent(
            uuid, patientUuid, instructions, start, durationDays));
    }

    public void onStopNow(DialogInterface dialog, final String orderUuid) {
        dialog.dismiss();

        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.title_confirmation)
            .setMessage(R.string.confirm_order_stop)
            .setPositiveButton(R.string.order_stop_now,
                (d, i) -> EventBus.getDefault().post(new OrderStopRequestedEvent(orderUuid)))
            .setNegativeButton(R.string.cancel, null)
            .create().show();
    }

    public void onDelete(DialogInterface dialog, final String orderUuid) {
        dialog.dismiss();

        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.title_confirmation)
            .setMessage(R.string.confirm_order_delete)
            .setPositiveButton(R.string.delete,
                (d, i) -> EventBus.getDefault().post(new OrderDeleteRequestedEvent(orderUuid)))
            .setNegativeButton(R.string.cancel, null)
            .create().show();
    }

    private void setError(EditText field, int resourceId, Object... args) {
        field.setError(getString(resourceId, args));
        field.invalidate();
        field.requestFocus();
    }

    private static void addClearButton(final TextView view, int drawableId) {
        final Drawable icon = view.getResources().getDrawable(drawableId);
        icon.setColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY);  // draw icon in black
        final int iw = icon.getIntrinsicWidth();
        final int ih = icon.getIntrinsicHeight();
        icon.setBounds(0, 0, iw, ih);

        final Drawable cd[] = view.getCompoundDrawables();
        view.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override public void afterTextChanged(Editable s) {
                boolean show = view.getText().length() > 0;
                view.setCompoundDrawables(cd[0], cd[1], show ? icon : cd[2], cd[3]);
            }
        });

        view.setMinimumHeight(view.getPaddingTop() + ih + view.getPaddingBottom());
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                if (x >= view.getWidth() - view.getPaddingRight() - iw && x < view.getWidth() &&
                    y >= 0 && y < view.getHeight()) {
                    view.setText("");
                    return true;
                }
            }
            return false;
        });
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = u.inflateForDialog(R.layout.order_dialog_fragment);
        ButterKnife.inject(this, fragment);

        Bundle args = getArguments();
        boolean newOrder = args.getBoolean("new");
        boolean stopped = args.getBoolean("stopped");
        boolean executed = args.getBoolean("executed");
        mOrderUuid = args.getString("uuid");
        populateFields(args);

        String title = getString(newOrder ? R.string.title_new_order : R.string.title_edit_order);

        // If the order has stopped, you can't change the frequency or duration.
        if (stopped) {
            mFrequency.setEnabled(false);
            mGiveForDays.setEnabled(false);
        }

        // Hide or show the "Stop" and "Delete" buttons appropriately.
        Long stopMillis = Utils.getLong(args, "stop_millis");
        Long nowMillis = Utils.getLong(args, "now_millis");
        Utils.showIf(mStopNow, !newOrder && !stopped);
        Utils.showIf(mDelete, !newOrder && !executed);

        addListeners();
        addClearButton(mMedication, R.drawable.abc_ic_clear_mtrl_alpha);
        mMedication.setThreshold(1);
        mMedication.setAdapter(new AutocompleteAdapter(
            getActivity(), R.layout.captioned_item, new MedCompleter()));

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setCancelable(false)
            .setTitle(title)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .setView(fragment)
            .create();

        // To prevent the dialog from being automatically dismissed, we have to
        // override the listener instead of passing it in to setPositiveButton.
        dialog.setOnShowListener(di ->
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> onSubmit(dialog))
        );

        return dialog;
    }


    /** Updates the various labels in the form that react to changes in input fields. */
    void updateLabels() {
        boolean newOrder = getArguments().getBoolean("new");
        int frequency = Utils.toIntOrDefault(mFrequency.getText().toString().trim(), 0);
        DateTime start = Utils.getDateTime(getArguments(), "start_millis");
        DateTime now = Utils.getDateTime(getArguments(), "now_millis");
        String text = mGiveForDays.getText().toString().trim();
        int days = text.isEmpty() ? 0 : Integer.parseInt(text);
        LocalDate startDay = (start != null ? start : now).toLocalDate();
        LocalDate stopDay = startDay.plusDays(days);
        boolean stopped = getArguments().getBoolean("stopped");

        // You can't specify a duration if the order isn't a series.
        mGiveForDays.setEnabled(frequency > 0);
        if (frequency == 0) mGiveForDays.setText("");

        mTimesPerDayLabel.setText(
            frequency == 1 ? R.string.order_time_per_day : R.string.order_times_per_day);

        mGiveForDaysLabel.setText(
            days == 1 ? R.string.order_give_for_day : R.string.order_give_for_days);

        int doses = frequency * days;
        String startDate = friendlyDateFormat(startDay);
        String stopDate = friendlyDateFormat(stopDay);

        if (newOrder) {
            mDurationLabel.setText(
                frequency == 0 || frequency * days == 1 ?
                    u.str(R.string.order_one_dose_only) :
                days == 0 ?
                    u.str(R.string.order_start_now_indefinitely) :
                days == 1 ?
                    u.str(R.string.order_start_now_stop_after_doses, doses) :
                u.str(R.string.order_start_now_after_doses_stop_date, doses, stopDate)
            );
        } else if (stopped) {
            mDurationLabel.setText(
                frequency == 0 || frequency * days == 1 ?
                    u.str(R.string.order_one_dose_only_ordered_date, startDate) :
                u.str(R.string.order_started_date_stopped_date, startDate, stopDate)
            );
        } else {
            mDurationLabel.setText(
                frequency == 0 || frequency * days == 1 ?
                    u.str(R.string.order_one_dose_only_ordered_date, startDate) :
                days == 0 ?
                    u.str(R.string.order_started_date_indefinitely, startDate) :
                days == 1 ?
                    u.str(R.string.order_started_date_stop_after_doses, startDate, doses) :
                u.str(R.string.order_started_date_after_doses_stop_date, startDate, doses, stopDate)
            );
        }
    }

    private String friendlyDateFormat(LocalDate date) {
        int days = Days.daysBetween(LocalDate.now(), date).getDays();
        return days == 0 ? u.str(R.string.sentence_today)
            : days == 1 ? u.str(R.string.sentence_tomorrow)
            : days == -1 ? u.str(R.string.sentence_yesterday)
            : Utils.format(date, SENTENCE_MONTH_DAY);
    }
}
