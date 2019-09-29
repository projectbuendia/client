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
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import org.projectbuendia.client.events.actions.OrderAddRequestedEvent;
import org.projectbuendia.client.events.actions.OrderDeleteRequestedEvent;
import org.projectbuendia.client.events.actions.OrderStopRequestedEvent;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.ui.AutocompleteAdapter;
import org.projectbuendia.client.ui.EditTextWatcher;
import org.projectbuendia.client.ui.MedCompleter;
import org.projectbuendia.client.utils.Utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.utils.Utils.DateStyle.SENTENCE_MONTH_DAY;

/** A DialogFragment for creating or editing a treatment order. */
public class OrderDialogFragment extends BaseDialogFragment<OrderDialogFragment, OrderDialogFragment.Args> {
    public static final int MAX_FREQUENCY = 24;  // maximum 24 times per day
    public static final int MAX_DURATION_DAYS = 30;  // maximum 30 days
    public static final String[] ROUTE_LABELS = {
        "PO", "IV", "IM", "SC"
    };
    // This should match the left/right padding in the TextViews in captioned_item.xml.
    private static final int ITEM_HORIZONTAL_PADDING = 12;

    static class Args implements Serializable {
        String patientUuid;
        Order order;
        boolean executed;
        boolean stopped;
        DateTime now;
    }

    class Views {
        AutoCompleteTextView medication = u.findView(R.id.order_medication);
        EditText route = u.findView(R.id.order_route);
        EditText dosage = u.findView(R.id.order_dosage);
        EditText frequency = u.findView(R.id.order_frequency);
        TextView timesPerDayLabel = u.findView(R.id.order_times_per_day_label);
        EditText giveForDays = u.findView(R.id.order_give_for_days);
        TextView giveForDaysLabel = u.findView(R.id.order_give_for_days_label);
        TextView durationLabel = u.findView(R.id.order_duration_label);
        EditText notes = u.findView(R.id.order_notes);
        Button stopNow = u.findView(R.id.order_stop_now);
        Button delete = u.findView(R.id.order_delete);
    }

    private Views v;
    private String orderUuid;
    private DateTime start;

    /** Creates a new instance and registers the given UI, if specified. */
    public static OrderDialogFragment create(
        String patientUuid, Order order, List<Obs> executions) {
        Args args = new Args();

        // This time is used as the current time for all calculations in this dialog.
        // Always use this value instead of calling now(), in order to maintain UI
        // consistency (e.g. if opened before midnight and submitted after midnight).
        args.now = DateTime.now();

        args.patientUuid = patientUuid;
        args.order = order;
        args.executed = Utils.hasItems(executions);
        args.stopped = order != null && (
            !order.isSeries() || (order.stop != null && args.now.isAfter(order.stop)));
        return new OrderDialogFragment().withArgs(args);
    }

    @Override public AlertDialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.order_dialog_fragment);
    }

    @Override public void onOpen() {
        v = new Views();

        orderUuid = args.order != null ? args.order.uuid : null;
        start = args.order != null ? args.order.start : args.now;

        // Populate the dialog appropriately.
        dialog.setTitle(args.order == null ? R.string.title_new_order : R.string.title_edit_order);
        if (args.order != null) populateFields();
        updateUi();

        // Finish building the dialog's behaviour.
        addClearButton(v.medication, R.drawable.abc_ic_clear_mtrl_alpha);
        v.medication.setThreshold(1);
        v.medication.setAdapter(new AutocompleteAdapter(
            getActivity(), R.layout.captioned_item, new MedCompleter()));

        // Open the keyboard, ready to type into the medication field.
        // TODO(ping): Figure out why this doesn't open the keyboard.
        v.medication.requestFocus();

        addListeners();
    }

    private void addListeners() {
        // TODO(ping): Replace the route EditText with a properly styled Spinner.
        v.route.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) v.callOnClick();
        });
        v.route.setOnClickListener(v -> {
            int index = Arrays.asList(ROUTE_LABELS).indexOf(this.v.route.getText().toString());
            new AlertDialog.Builder(getActivity())
                .setTitle(R.string.route_of_administration)
                .setSingleChoiceItems(ROUTE_LABELS, index, (dialog, which) -> {
                    this.v.route.setText(ROUTE_LABELS[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
        });
        // After the dialog has been laid out and positioned, we can figure out
        // how to position and size the autocompletion dropdown.
        v.medication.getViewTreeObserver().addOnPreDrawListener(() -> {
            adjustDropDownSize(v.medication, ITEM_HORIZONTAL_PADDING);
            return true;
        });

        new EditTextWatcher(v.frequency, v.giveForDays).onChange(this::updateUi);
        v.stopNow.setOnClickListener(view -> onStopNow());
        v.delete.setOnClickListener(view -> onDelete());
    }

    private void populateFields() {
        v.medication.setText(args.order.instructions.medication);
        v.route.setText(args.order.instructions.route);
        v.dosage.setText(args.order.instructions.dosage);
        int frequency = args.order.instructions.frequency;
        v.frequency.setText(frequency > 0 ? Integer.toString(frequency) : "");
        v.notes.setText(args.order.instructions.notes);
        if (args.order.stop != null) {
            int days = Days.daysBetween(start.toLocalDate(), args.order.stop.toLocalDate()).getDays();
            if (days > 0) v.giveForDays.setText("" + days);
        }
    }

    @Override protected void onSubmit() {
        String medication = v.medication.getText().toString().trim();
        String route = v.route.getText().toString().trim();
        String dosage = v.dosage.getText().toString().trim();
        int frequency = Utils.toIntOrDefault(v.frequency.getText().toString().trim(), 0);
        String notes = v.notes.getText().toString().trim();
        Order.Instructions instructions = new Order.Instructions(medication, route, dosage, frequency, notes);

        String durationStr = v.giveForDays.getText().toString().trim();
        Integer durationDays = durationStr.isEmpty() ? null : Integer.valueOf(durationStr);
        boolean valid = true;
        if (medication.isEmpty()) {
            setError(v.medication, R.string.enter_medication);
            valid = false;
        }
        if (frequency > MAX_FREQUENCY) {
            setError(v.frequency, R.string.order_cannot_exceed_n_times_per_day, MAX_FREQUENCY);
            valid = false;
        }
        if (durationDays != null) {
            if (durationDays == 0) {
                setError(v.giveForDays, R.string.order_give_for_days_cannot_be_zero);
                valid = false;
            }
            if (durationDays > MAX_DURATION_DAYS) {
                setError(v.giveForDays, R.string.order_cannot_exceed_n_days, MAX_DURATION_DAYS);
                valid = false;
            }
            if (start.plusDays(durationDays).isBefore(args.now)) {
                setError(v.giveForDays, R.string.order_cannot_stop_in_past);
                valid = false;
            }
        }
        Utils.logUserAction("order_submitted",
            "valid", "" + valid,
            "uuid", orderUuid,
            "medication", medication,
            "route", route,
            "dosage", dosage,
            "frequency", "" + frequency,
            "notes", notes,
            "durationDays", "" + durationDays);
        if (!valid) return;

        dialog.dismiss();

        // Post an event that triggers the PatientChartController to save the order.
        EventBus.getDefault().post(new OrderAddRequestedEvent(
            orderUuid, args.patientUuid, Utils.getProviderUuid(), instructions, start, durationDays
        ));
    }

    private void onStopNow() {
        dialog.dismiss();
        u.prompt(R.string.title_confirmation, R.string.confirm_order_stop, R.string.order_stop_now,
            () -> EventBus.getDefault().post(new OrderStopRequestedEvent(orderUuid)));
    }

    private void onDelete() {
        dialog.dismiss();
        u.prompt(R.string.title_confirmation, R.string.confirm_order_delete, R.string.delete,
            () -> EventBus.getDefault().post(new OrderDeleteRequestedEvent(orderUuid)));
    }

    /** Adds an "X" button to a text edit field. */
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

    /** Updates labels and disables or hides elements according to the input fields. */
    private void updateUi() {
        int frequency = Utils.toIntOrDefault(v.frequency.getText().toString().trim(), 0);

        String text = v.giveForDays.getText().toString().trim();
        int days = text.isEmpty() ? 0 : Integer.parseInt(text);
        LocalDate startDay = start.toLocalDate();
        LocalDate stopDay = startDay.plusDays(days);

        if (frequency == 0) v.giveForDays.setText("");
        v.timesPerDayLabel.setText(
            frequency == 1 ? R.string.order_time_per_day : R.string.order_times_per_day);
        v.giveForDaysLabel.setText(
            days == 1 ? R.string.order_give_for_day : R.string.order_give_for_days);

        int doses = frequency * days;
        String startDate = friendlyDateFormat(startDay);
        String stopDate = friendlyDateFormat(stopDay);

        if (args.order == null) {
            v.durationLabel.setText(
                frequency == 0 || frequency * days == 1 ?
                    u.str(R.string.order_one_dose_only) :
                days == 0 ?
                    u.str(R.string.order_start_now_indefinitely) :
                days == 1 ?
                    u.str(R.string.order_start_now_stop_after_doses, doses) :
                u.str(R.string.order_start_now_after_doses_stop_date, doses, stopDate)
            );
        } else if (args.stopped) {
            v.durationLabel.setText(
                frequency == 0 || frequency * days == 1 ?
                    u.str(R.string.order_one_dose_only_ordered_date, startDate) :
                u.str(R.string.order_started_date_stopped_date, startDate, stopDate)
            );
        } else {
            v.durationLabel.setText(
                frequency == 0 || frequency * days == 1 ?
                    u.str(R.string.order_one_dose_only_ordered_date, startDate) :
                days == 0 ?
                    u.str(R.string.order_started_date_indefinitely, startDate) :
                days == 1 ?
                    u.str(R.string.order_started_date_stop_after_doses, startDate, doses) :
                u.str(R.string.order_started_date_after_doses_stop_date, startDate, doses, stopDate)
            );
        }

        // If the order has stopped, you can't change the frequency or duration.
        Utils.setEnabled(v.frequency, !args.stopped);
        // You can't specify a duration if the order isn't a series.
        Utils.setEnabled(v.giveForDays, !args.stopped && frequency > 0);

        // Hide or show the "Stop" and "Delete" buttons appropriately.
        Utils.showIf(v.stopNow, args.order != null && !args.stopped);
        Utils.showIf(v.delete, args.order != null && !args.executed);
    }

    private String friendlyDateFormat(LocalDate date) {
        int days = Days.daysBetween(LocalDate.now(), date).getDays();
        return days == 0 ? u.str(R.string.sentence_today)
            : days == 1 ? u.str(R.string.sentence_tomorrow)
            : days == -1 ? u.str(R.string.sentence_yesterday)
            : Utils.format(date, SENTENCE_MONTH_DAY);
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
        textView.setDropDownWidth(v.medication.getWidth() + itemHorizontalPadding * 2);
    }
}
