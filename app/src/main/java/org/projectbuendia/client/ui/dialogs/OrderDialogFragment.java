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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.OrderDeleteRequestedEvent;
import org.projectbuendia.client.events.actions.OrderSaveRequestedEvent;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.ui.AutocompleteAdapter;
import org.projectbuendia.client.ui.MedCompleter;
import org.projectbuendia.client.utils.Utils;

import java.util.Arrays;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

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
    @InjectView(R.id.order_give_for_days) EditText mGiveForDays;
    @InjectView(R.id.order_give_for_days_label) TextView mGiveForDaysLabel;
    @InjectView(R.id.order_duration_label) TextView mDurationLabel;
    @InjectView(R.id.order_notes) EditText mNotes;
    @InjectView(R.id.order_delete) Button mDelete;

    private LayoutInflater mInflater;
    private String mOrderUuid;

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
            args.putString("medication", order.instructions.medication);
            args.putString("route", order.instructions.route);
            args.putString("dosage", order.instructions.dosage);
            args.putInt("frequency", order.instructions.frequency);
            args.putString("notes", order.instructions.notes);
            Utils.putDateTime(args, "start_millis", order.start);
            Utils.putDateTime(args, "stop_millis", order.stop);
        }
        OrderDialogFragment fragment = new OrderDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
    }

    @Override public void onResume() {
        super.onResume();

        // Open the keyboard, ready to type into the medication field.
        mMedication.requestFocus();

        // After the dialog has been laid out and positioned, we can figure out
        // how to position and size the autocompletion dropdown.
        mMedication.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                adjustDropDownSize(mMedication, ITEM_HORIZONTAL_PADDING);
                return true;
            }
        });
    }

    private void populateFields(Bundle args) {
        mMedication.setText(args.getString("medication"));
        mRoute.setText(args.getString("route"));
        mDosage.setText(args.getString("dosage"));
        int frequency = args.getInt("frequency");
        mFrequency.setText(frequency > 0 ? Integer.toString(frequency) : "");
        mNotes.setText(args.getString("notes"));
        DateTime now = Utils.getDateTime(args, "now_millis");
        DateTime stop = Utils.getDateTime(args, "stop_millis");
        if (stop != null) {
            LocalDate lastDay = stop.toLocalDate();
            // TODO(ping): Orders that stopped in the past will have a blank duration.
            int days = Days.daysBetween(now.toLocalDate(), lastDay).getDays();
            if (days >= 0) {
                mGiveForDays.setText(Utils.format("%d", days + 1));  // 1 day means stop after today
            }
        }
        updateLabels();
    }

    private void addListeners() {
        // TODO(ping): Replace the mRoute EditText with a properly styled Spinner.
        mRoute.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) v.callOnClick();
            }
        });
        mRoute.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                int index = Arrays.asList(ROUTE_LABELS).indexOf(mRoute.getText().toString());
                new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.route_of_administration)
                    .setSingleChoiceItems(ROUTE_LABELS, index, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mRoute.setText(ROUTE_LABELS[which]);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            }
        });

        mGiveForDays.addTextChangedListener(new DurationDaysWatcher());

        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                onDelete(getDialog(), mOrderUuid);
            }
        });
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
        if (durationDays != null && durationDays == 0) {
            setError(mGiveForDays, R.string.order_give_for_days_cannot_be_zero);
            valid = false;
        }
        if (durationDays != null && durationDays > MAX_DURATION_DAYS) {
            setError(mGiveForDays, R.string.order_cannot_exceed_n_days, MAX_DURATION_DAYS);
            valid = false;
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

        DateTime start = Utils.getDateTime(getArguments(), "start_millis");
        DateTime now = Utils.getDateTime(getArguments(), "now_millis");
        start = Utils.orDefault(start, now);

        if (durationDays != null) {
            // Adjust durationDays to account for a start date in the past.  Entering "2"
            // always means two more days, stopping after tomorrow, regardless of start date.
            LocalDate firstDay = start.toLocalDate();
            LocalDate lastDay = now.toLocalDate().plusDays(durationDays - 1);
            durationDays = Days.daysBetween(firstDay, lastDay).getDays() + 1;
        }

        // Post an event that triggers the PatientChartController to save the order.
        EventBus.getDefault().post(new OrderSaveRequestedEvent(
            uuid, patientUuid, instructions, start, durationDays));
    }

    public void onDelete(DialogInterface dialog, final String orderUuid) {
        dialog.dismiss();

        new AlertDialog.Builder(getActivity())
            .setMessage(R.string.confirm_order_delete)
            .setTitle(R.string.title_confirmation)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int i) {
                    EventBus.getDefault().post(new OrderDeleteRequestedEvent(orderUuid));
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .create().show();
    }

    private void setError(EditText field, int resourceId, Object... args) {
        field.setError(getResources().getString(resourceId, args));
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
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
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
            }
        });
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.order_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        Bundle args = getArguments();
        boolean newOrder = args.getBoolean("new");
        String title = getString(newOrder ? R.string.title_new_order : R.string.title_edit_order);
        mOrderUuid = args.getString("uuid");
        populateFields(args);

        addListeners();
        addClearButton(mMedication, R.drawable.abc_ic_clear_mtrl_alpha);
        mMedication.setThreshold(1);
        mMedication.setAdapter(new AutocompleteAdapter(
            getActivity(), R.layout.captioned_item, new MedCompleter()));

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setCancelable(false)
            .setTitle(title)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which) {
                    onSubmit(dialog);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .setView(fragment)
            .create();

        // Hide or show the "Stop" and "Delete" buttons appropriately.
        Long stopMillis = Utils.getLong(args, "stop_millis");
        Long nowMillis = Utils.getLong(args, "now_millis");
        Utils.showIf(mDelete, !newOrder);

        return dialog;
    }


    /** Updates the various labels in the form that react to changes in input fields. */
    void updateLabels() {
        int frequency = Utils.toIntOrDefault(mFrequency.getText().toString().trim(), 0);
        DateTime now = Utils.getDateTime(getArguments(), "now_millis");
        String text = mGiveForDays.getText().toString().trim();
        int days = text.isEmpty() ? 0 : Integer.parseInt(text);
        LocalDate lastDay = now.toLocalDate().plusDays(days - 1);
        mGiveForDaysLabel.setText(
            days == 0 ? R.string.order_give_for_days :
                days == 1 ? R.string.order_give_for_day :
                    R.string.order_give_for_days);
        mDurationLabel.setText(getResources().getString(
            days == 0 ? (
                frequency == 0 ?
                R.string.order_duration_administer_once :
                R.string.order_duration_indefinitely
            ) :
                days == 1 ? R.string.order_duration_stop_after_today :
                    days == 2 ? R.string.order_duration_stop_after_tomorrow :
                        R.string.order_duration_stop_after_date
        ).replace("%s", Utils.formatShortDate(lastDay)));
    }

    class DurationDaysWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence c, int x, int y, int z) { }

        @Override public void onTextChanged(CharSequence c, int x, int y, int z) { }

        @Override public void afterTextChanged(Editable editable) {
            updateLabels();
        }
    }

}
