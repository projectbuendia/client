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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.common.base.Joiner;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.OrderExecutionAddRequestedEvent;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.utils.Utils.DateStyle.HOUR_MINUTE;
import static org.projectbuendia.client.utils.Utils.DateStyle.MONTH_DAY;

/** A {@link DialogFragment} for recording that an order was executed. */
public class OrderExecutionDialogFragment extends DialogFragment {
    @InjectView(R.id.order_medication) TextView mOrderMedication;
    @InjectView(R.id.order_dosage) TextView mOrderDosage;
    @InjectView(R.id.order_notes) TextView mOrderNotes;
    @InjectView(R.id.order_start_time) TextView mOrderStartTime;
    @InjectView(R.id.execution_count) TextView mExecutionCount;
    @InjectView(R.id.execution_list) TextView mExecutionList;
    @InjectView(R.id.execute_toggle) ToggleButton mExecuteToggle;
    private ContextUtils u;

    /** Creates a new instance and registers the given UI, if specified. */
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
        args.putLong("intervalStartMillis", interval.getStartMillis());
        args.putLong("intervalStopMillis", interval.getEndMillis());
        List<Long> millis = new ArrayList<>();
        for (Obs obs : executions) {
            if (interval.contains(obs.time)) {
                millis.add(obs.time.getMillis());
            }
        }
        args.putLongArray("executionTimes", Utils.toArray(millis));
        // To avoid the possibility of confusion when the dialog is opened just
        // before midnight, save the current time for use as the encounter time later.
        DateTime executionTime = DateTime.now();
        args.putLong("executionTimeMillis", executionTime.getMillis());
        args.putBoolean("editable", interval.contains(executionTime));
        OrderExecutionDialogFragment f = new OrderExecutionDialogFragment();
        f.setArguments(args);
        return f;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        u = ContextUtils.from(getActivity());
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = u.inflateForDialog(R.layout.order_execution_dialog_fragment);
        ButterKnife.inject(this, fragment);

        updateUi(false);
        mExecuteToggle.setOnCheckedChangeListener((compoundButton, checked) -> updateUi(checked));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            .setTitle(getString(R.string.order_execution_title))
            .setPositiveButton(R.string.ok, (dialogInterface, i) -> onSubmit())
            .setNegativeButton(R.string.cancel, null)
            .setView(fragment);

        if (!getArguments().getBoolean("editable")) {
            // Historical counts can be viewed but not changed.
            builder.setNegativeButton(null, null);
            mExecuteToggle.setVisibility(View.GONE);
        }
        return builder.create();
    }

    void updateUi(boolean orderExecutedNow) {
        Bundle args = getArguments();
        LocalDate date = new DateTime(Utils.getLong(args, "intervalStartMillis")).toLocalDate();
        List<DateTime> executionTimes = new ArrayList<>();
        for (long millis : args.getLongArray("executionTimes")) {
            executionTimes.add(new DateTime(millis));
        }
        Collections.sort(executionTimes);

        // Show what was ordered and when the order started.
        mOrderMedication.setText(getString(
            R.string.order_medication_route,
            args.getString("medication"),
            args.getString("route")
        ));
        int frequency = args.getInt("frequency");
        String dosage = args.getString("dosage");
        if (Utils.isBlank(dosage)) dosage = u.str(R.string.order_unspecified_dosage);
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

        // Describe how many times the order was executed during the selected interval.
        int count = executionTimes.size() + (orderExecutedNow ? 1 : 0);
        boolean plural = count != 1;
        mExecutionCount.setText(Html.fromHtml(getString(
            date.equals(LocalDate.now()) ?
                (plural ? R.string.order_execution_today_plural_html
                    : R.string.order_execution_today_singular_html) :
                (plural ? R.string.order_execution_historical_plural_html
                    : R.string.order_execution_historical_singular_html),
            count, Utils.format(date, MONTH_DAY))));

        // Show the list of times that the order was executed during the selected interval.
        boolean editable = args.getBoolean("editable");
        Utils.showIf(mExecutionList, executionTimes.size() > 0 || editable);
        List<String> htmlItems = new ArrayList<>();
        for (DateTime executionTime : executionTimes) {
            htmlItems.add(Utils.format(executionTime, HOUR_MINUTE));
        }
        if (editable) {
            DateTime executionTime = Utils.getDateTime(args, "executionTimeMillis");
            htmlItems.add(orderExecutedNow ?
                "<b>" + Utils.format(executionTime, HOUR_MINUTE) + "</b>" :
                "<b>&nbsp;</b>");  // keep total height stable
        }
        mExecutionList.setText(Html.fromHtml(Joiner.on("<br>").join(htmlItems)));
    }

    public void onSubmit() {
        if (mExecuteToggle.isChecked()) {
            Bundle args = getArguments();
            String orderUuid = args.getString("orderUuid");
            String instructions = args.getString("instructions");
            Interval interval = new Interval(
                Utils.getDateTime(args, "intervalStartMillis"),
                Utils.getDateTime(args, "intervalStopMillis"));
            DateTime executionTime = Utils.getDateTime(args, "executionTimeMillis");
            Utils.logUserAction("order_execution_submitted",
                "orderUuid", orderUuid,
                "instructions", instructions,
                "interval", "" + interval,
                "executionTime", "" + executionTime);

            // Post an event that triggers the PatientChartController to record the order execution.
            EventBus.getDefault().post(
                new OrderExecutionAddRequestedEvent(orderUuid, executionTime));
        }
    }
}
