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
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.common.base.Joiner;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.ObsDeleteRequestedEvent;
import org.projectbuendia.client.events.actions.OrderExecutionAddRequestedEvent;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.utils.Utils.DateStyle.HOUR_MINUTE;
import static org.projectbuendia.client.utils.Utils.DateStyle.SENTENCE_MONTH_DAY;

/** A {@link DialogFragment} for recording that an order was executed. */
public class OrderExecutionDialogFragment extends DialogFragment {
    @InjectView(R.id.order_medication) TextView mOrderMedication;
    @InjectView(R.id.order_dosage) TextView mOrderDosage;
    @InjectView(R.id.order_notes) TextView mOrderNotes;
    @InjectView(R.id.order_start_time) TextView mOrderStartTime;
    @InjectView(R.id.execution_count) TextView mExecutionCount;
    @InjectView(R.id.execution_list) ViewGroup mExecutionList;
    @InjectView(R.id.execute_toggle) ToggleButton mExecuteToggle;
    @InjectView(R.id.delete_button) Button mDeleteButton;
    private ContextUtils u;
    private List<View> mItems;
    private View mNewItem;
    private Set<String> mObsUuidsToDelete = new HashSet<>();

    /** Creates a new instance showing a list of executions in the order given. */
    public static OrderExecutionDialogFragment newInstance(
        Order order, Interval interval, List<Obs> executions) {
        Bundle args = new Bundle();
        args.putString("orderUuid", order.uuid);
        args.putString("medication", order.instructions.medication);
        args.putString("route", order.instructions.route);
        args.putString("dosage", order.instructions.dosage);
        args.putInt("frequency", order.instructions.frequency);
        args.putString("notes", order.instructions.notes);
        args.putLong("orderStartMillis", order.start.getMillis());
        args.putSerializable("date", interval.getStart().toLocalDate());
        ArrayList<Obs> executionsInInterval = new ArrayList<>();
        for (Obs obs : executions) {
            if (interval.contains(obs.time)) executionsInInterval.add(obs);
        }
        args.putParcelableArrayList("executions", executionsInInterval);
        // To avoid the possibility of confusion when the dialog is opened just
        // before midnight, save the current time for use as the encounter time later.
        DateTime executionTime = DateTime.now();
        args.putLong("executionTimeMillis", executionTime.getMillis());
        args.putBoolean("executable", interval.contains(executionTime));
        OrderExecutionDialogFragment f = new OrderExecutionDialogFragment();
        f.setArguments(args);
        return f;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        u = ContextUtils.from(getActivity());
        mObsUuidsToDelete.clear();
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = u.inflateForDialog(R.layout.order_execution_dialog_fragment);
        ButterKnife.inject(this, fragment);

        populateUi();
        updateUi();
        mExecuteToggle.setOnCheckedChangeListener((button, checked) -> updateUi());
        mDeleteButton.setOnClickListener(view -> deleteSelected());

        return new AlertDialog.Builder(getActivity())
            .setTitle(getString(R.string.order_execution_title))
            .setPositiveButton(R.string.ok, (di, which) -> onSubmit())
            .setNegativeButton(R.string.cancel, null)
            .setView(fragment)
            .create();
    }

    private void populateUi() {
        Bundle args = getArguments();

        // Show what was ordered and when the order started.
        mOrderMedication.setText(getString(
            R.string.order_medication_route,
            args.getString("medication"),
            args.getString("route")
        ));
        int frequency = args.getInt("frequency");
        String dosage = args.getString("dosage");
        if (Utils.isBlank(dosage))
            dosage = u.str(R.string.order_unspecified_dosage);
        mOrderDosage.setText(getString(
            frequency > 0 ? R.string.order_dosage_series : R.string.order_dosage_unary,
            dosage, frequency
        ));
        String notes = args.getString("notes");
        mOrderNotes.setText(notes);
        mOrderNotes.setVisibility(notes.trim().isEmpty() ? View.GONE : View.VISIBLE);

        DateTime start = Utils.getDateTime(args, "orderStartMillis");
        mOrderStartTime.setText(getString(
            R.string.order_started_datetime,
            Utils.format(start, Utils.DateStyle.SENTENCE_MONTH_DAY_HOUR_MINUTE)));

        // Populate the list of execution times with checkable items.
        boolean executable = args.getBoolean("executable");

        List<Obs> executions = args.getParcelableArrayList("executions");
        Utils.showIf(mExecutionList, executions.size() > 0 || executable);

        mItems = new ArrayList<>();
        for (Obs obs : executions) {
            View item = u.inflate(R.layout.checkable_item, mExecutionList);
            u.setText(R.id.text, Utils.format(obs.time, HOUR_MINUTE));
            mExecutionList.addView(item);
            final CheckBox checkbox = u.findView(R.id.checkbox);
            checkbox.setOnCheckedChangeListener((view, checked) -> updateUi());
            item.setOnClickListener(view -> {
                if (checkbox.isEnabled()) checkbox.setChecked(!checkbox.isChecked());
            });
            item.setTag(obs);
            mItems.add(item);
        }
        if (executable) {
            mNewItem = u.inflate(R.layout.checkable_item, mExecutionList);
            DateTime executionTime = Utils.getDateTime(args, "executionTimeMillis");
            TextView text = u.findView(R.id.text);
            text.setText(Utils.format(executionTime, HOUR_MINUTE));
            text.setTypeface(text.getTypeface(), Typeface.BOLD);
            u.findView(R.id.checkbox).setVisibility(View.INVISIBLE);
            mExecutionList.addView(mNewItem);
            mNewItem.setVisibility(View.INVISIBLE);
        }
    }

    /** Updates the UI to reflect the changes proposed by the user. */
    void updateUi() {
        Bundle args = getArguments();
        boolean executeNow = mExecuteToggle.isChecked();

        // Describe how many times the order was executed during the selected interval.
        int count = mItems.size() + (executeNow ? 1 : 0);
        boolean plural = count != 1;
        LocalDate date = (LocalDate) args.getSerializable("date");
        mExecutionCount.setText(Html.fromHtml(getString(
            date.equals(LocalDate.now()) ?
                (plural ? R.string.order_execution_today_plural_html
                    : R.string.order_execution_today_singular_html) :
                (plural ? R.string.order_execution_historical_plural_html
                    : R.string.order_execution_historical_singular_html),
            count, Utils.format(date, SENTENCE_MONTH_DAY))));

        // Update the list of execution times.
        for (View item : mItems) {
            Obs obs = (Obs) item.getTag();
            if (mObsUuidsToDelete.contains(obs.uuid)) {
                item.setBackgroundColor(0xffffcccc);
                ((TextView) item.findViewById(R.id.text)).setTextColor(0xff999999);
                item.findViewById(R.id.strikethrough).setVisibility(View.VISIBLE);
            }
        }
        if (mNewItem != null) {
            mNewItem.setVisibility(executeNow ? View.VISIBLE : View.INVISIBLE);
        }

        boolean executable = args.getBoolean("executable");
        boolean anyItemsChecked = false;
        for (View item : mItems) {
            CheckBox checkbox = item.findViewById(R.id.checkbox);
            if (checkbox.isChecked()) anyItemsChecked = true;
        }

        // To keep the dialog simple, we only show one button at a time (either
        // the Mark button or the Delete button).  To keep the dialog from
        // resizing, we always show one button.
        boolean showDeleteButton;
        if (anyItemsChecked || mObsUuidsToDelete.size() > 0) {
            showDeleteButton = true;
        } else if (executable) {
            showDeleteButton = false;
        } else {
            showDeleteButton = true;
        }
        Utils.showIf(mExecuteToggle, !showDeleteButton);
        Utils.showIf(mDeleteButton, showDeleteButton);

        // Special case: if neither button can ever be used, hide both.
        if (!executable && !Utils.hasItems(mItems)) {
            Utils.showIf(mExecuteToggle, false);
            Utils.showIf(mDeleteButton, false);
        }

        // Light up the Delete button only if it has an effect.
        mDeleteButton.setEnabled(anyItemsChecked);
        mDeleteButton.setBackgroundColor(anyItemsChecked ? 0xffff6666 : 0xffcccccc);

        // If execution is requested, prevent marking anything for deletion;
        // otherwise just disable the checkboxes of items already marked for deletion.
        if (mExecuteToggle.isChecked()) {
            for (View item : mItems) item.findViewById(R.id.checkbox).setEnabled(false);
        } else {
            for (View item : mItems) {
                Obs obs = (Obs) item.getTag();
                item.findViewById(R.id.checkbox).setEnabled(
                    !mObsUuidsToDelete.contains(obs.uuid));
            }
        }
    }

    /** Marks the checked items for deletion. */
    private void deleteSelected() {
        for (View item : mItems) {
            Obs obs = (Obs) item.getTag();
            CheckBox checkbox = item.findViewById(R.id.checkbox);
            if (checkbox.isChecked()) {
                mObsUuidsToDelete.add(obs.uuid);
                checkbox.setChecked(false);
            }
        }
        updateUi();
    }

    /** Applies the requested new execution or deletions. */
    public void onSubmit() {
        Bundle args = getArguments();
        String orderUuid = args.getString("orderUuid");
        String instructions = args.getString("instructions");
        DateTime executionTime = Utils.getDateTime(args, "executionTimeMillis");

        if (mExecuteToggle.isChecked()) {
            Utils.logUserAction("order_execution_submitted",
                "orderUuid", orderUuid,
                "instructions", instructions,
                "executionTime", "" + executionTime);

            EventBus.getDefault().post(
                new OrderExecutionAddRequestedEvent(orderUuid, executionTime));
        }

        List<Obs> executions = args.getParcelableArrayList("executions");
        List<Obs> executionsToDelete = new ArrayList<>();
        for (Obs obs : executions) {
            if (mObsUuidsToDelete.contains(obs.uuid)) {
                executionsToDelete.add(obs);
            }
        }

        if (mObsUuidsToDelete.size() > 0) {
            Utils.logUserAction("order_execution_deleted",
                "orderUuid", orderUuid,
                "obsUuids", Joiner.on(",").join(mObsUuidsToDelete));
            EventBus.getDefault().post(
                new ObsDeleteRequestedEvent(executionsToDelete));
        }
    }
}
