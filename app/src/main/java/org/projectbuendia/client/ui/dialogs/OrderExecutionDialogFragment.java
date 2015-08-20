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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.common.base.Joiner;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.OrderExecutionSaveRequestedEvent;
import org.projectbuendia.client.sync.Order;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/** A {@link DialogFragment} for recording that an order was executed. */
public class OrderExecutionDialogFragment extends DialogFragment {
    /** Creates a new instance and registers the given UI, if specified. */
    public static OrderExecutionDialogFragment newInstance(
            Order order, Interval interval, List<DateTime> executionTimes) {
        Bundle args = new Bundle();
        args.putString("orderUuid", order.uuid);
        args.putString("instructions", order.instructions);
        args.putLong("orderStartMillis", order.start.getMillis());
        args.putLong("intervalStartMillis", interval.getStartMillis());
        args.putLong("intervalStopMillis", interval.getEndMillis());
        List<Long> millis = new ArrayList<>();
        for (DateTime dt : executionTimes) {
            if (interval.contains(dt)) {
                millis.add(dt.getMillis());
            }
        }
        args.putLongArray("executionTimes", Utils.toArray(millis));
        // To avoid the possibility of confusion when the dialog is opened just
        // before midnight, save the current time for use as the encounter time later.
        DateTime encounterTime = DateTime.now();
        args.putLong("encounterTimeMillis", encounterTime.getMillis());
        args.putBoolean("editable", interval.contains(encounterTime));
        OrderExecutionDialogFragment f = new OrderExecutionDialogFragment();
        f.setArguments(args);
        return f;
    }

    @InjectView(R.id.order_instructions) TextView mOrderInstructions;
    @InjectView(R.id.order_start_time) TextView mOrderStartTime;
    @InjectView(R.id.order_execution_count) TextView mOrderExecutionCount;
    @InjectView(R.id.order_execution_list) TextView mOrderExecutionList;
    @InjectView(R.id.order_execution_mark_toggle) ToggleButton mMarkToggle;

    private LayoutInflater mInflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
    }

    public void onSubmit() {
        if (mMarkToggle.isChecked()) {
            Bundle args = getArguments();
            String orderUuid = args.getString("orderUuid");
            String instructions = args.getString("instructions");
            Interval interval = new Interval(
                    args.getLong("intervalStartMillis"),
                    args.getLong("intervalStopMillis"));
            DateTime encounterTime = new DateTime(args.getLong("encounterTimeMillis"));
            Utils.logUserAction("order_execution_submitted",
                    "orderUuid", orderUuid,
                    "instructions", instructions,
                    "interval", "" + interval,
                    "encounterTime", "" + encounterTime);

            // Post an event that triggers the PatientChartController to record the order execution.
            EventBus.getDefault().post(new OrderExecutionSaveRequestedEvent(
                    orderUuid, interval, encounterTime));
        }
    }

    void updateUi(boolean orderExecutedNow) {
        Bundle args = getArguments();
        LocalDate date = new DateTime(args.getLong("intervalStartMillis")).toLocalDate();
        List<DateTime> executionTimes = new ArrayList<>();
        for (long millis : args.getLongArray("executionTimes")) {
            executionTimes.add(new DateTime(millis));
        }

        // Show what was ordered and when the order started.
        mOrderInstructions.setText(args.getString("instructions"));
        DateTime start = new DateTime(args.getLong("orderStartMillis"));
        mOrderStartTime.setText(getResources().getString(
                R.string.order_started_on_date_at_time,
                Utils.toShortString(start.toLocalDate()), Utils.toTimeOfDayString(start)));

        // Describe how many times the order was executed during the selected interval.
        int count = executionTimes.size() + (orderExecutedNow ? 1 : 0);
        boolean plural = count != 1;
        mOrderExecutionCount.setText(Html.fromHtml(getResources().getString(
                date.equals(LocalDate.now()) ?
                        (plural ? R.string.order_execution_today_plural_html
                                : R.string.order_execution_today_singular_html) :
                        (plural ? R.string.order_execution_historical_plural_html
                                : R.string.order_execution_historical_singular_html),
                count, Utils.toShortString(date))));

        // Show the list of times that the order was executed during the selected interval.
        boolean editable = args.getBoolean("editable");
        Utils.showIf(mOrderExecutionList, executionTimes.size() > 0 || editable);
        List<String> htmlItems = new ArrayList<>();
        for (DateTime executionTime : executionTimes) {
            htmlItems.add(Utils.toTimeOfDayString(executionTime));
        }
        if (editable) {
            DateTime encounterTime = new DateTime(args.getLong("encounterTimeMillis"));
            htmlItems.add(orderExecutedNow ?
                    "<b>" + Utils.toTimeOfDayString(encounterTime) + "</b>" :
                    "<b>&nbsp;</b>");  // keep total height stable
        }
        mOrderExecutionList.setText(Html.fromHtml(Joiner.on("<br>").join(htmlItems)));
    }

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.order_execution_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        updateUi(false);
        mMarkToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                updateUi(checked);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(getResources().getString(R.string.order_execution_title))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onSubmit();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setView(fragment);

        if (!getArguments().getBoolean("editable")) {
            // Historical counts can be viewed but not changed.
            builder.setNegativeButton(null, null);
            mMarkToggle.setVisibility(View.GONE);
        }
        return builder.create();
    }
}