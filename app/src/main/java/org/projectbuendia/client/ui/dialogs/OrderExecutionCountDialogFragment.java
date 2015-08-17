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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.OrderExecutionCountSaveRequestedEvent;
import org.projectbuendia.client.sync.Order;
import org.projectbuendia.client.utils.Utils;

import java.awt.font.TextAttribute;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/** A {@link DialogFragment} for adding a new user. */
public class OrderExecutionCountDialogFragment extends DialogFragment {
    /** Creates a new instance and registers the given UI, if specified. */
    public static OrderExecutionCountDialogFragment newInstance(
            Order order, Interval interval, int currentExecutionCount) {
        Bundle args = new Bundle();
        args.putString("orderInstructions", order.instructions);
        args.putString("orderUuid", order.uuid);
        args.putLong("startMillis", interval.getStartMillis());
        args.putLong("stopMillis", interval.getEndMillis());
        args.putInt("currentExecutionCount", currentExecutionCount);
        OrderExecutionCountDialogFragment f = new OrderExecutionCountDialogFragment();
        f.setArguments(args);
        return f;
    }

    @InjectView(R.id.order_instructions) TextView mInstructions;
    @InjectView(R.id.order_execution_post_count_label) TextView mPostCountLabel;
    @InjectView(R.id.order_execution_count) EditText mExecutionCount;
    @InjectView(R.id.order_execution_count_minus) Button mMinusButton;
    @InjectView(R.id.order_execution_count_plus) Button mPlusButton;

    private LayoutInflater mInflater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
    }

    public void onSubmit(AlertDialog dialog) {
        int count = getExecutionCount();
        Utils.logUserAction("order_execution_submitted", "count", "" + count);

        dialog.dismiss();

        // Post an event that triggers the PatientChartController to save the count.
        EventBus.getDefault().post(new OrderExecutionCountSaveRequestedEvent(
                getArguments().getString("orderUuid"),
                new Interval(getArguments().getLong("startMillis"),
                        getArguments().getLong("stopMillis")),
                count));
    }

    int getExecutionCount() {
        String text = mExecutionCount.getText().toString().trim();
        return text.isEmpty() ? 0 : Integer.valueOf(text);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.order_execution_count_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        int count = getArguments().getInt("currentExecutionCount");
        String instructions = getArguments().getString("orderInstructions");
        LocalDate startDate = new DateTime(getArguments().getLong("startMillis")).toLocalDate();
        mInstructions.setText(instructions);
        mExecutionCount.setText(count > 0 ? "" + count : "");
        mPostCountLabel.setText(getResources().getString(
                startDate.equals(LocalDate.now()) ?
                    R.string.order_execution_post_count_label_today :
                    R.string.order_execution_post_count_label,
                Utils.toShortString(startDate)));

        mMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int count = getExecutionCount();
                mExecutionCount.setText("" + (count > 0 ? count - 1 : 0));
            }
        });
        mPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mExecutionCount.setText("" + (getExecutionCount() + 1));
            }
        });

        return new AlertDialog.Builder(getActivity())
                .setCancelable(false) // Disable auto-cancel.
                .setTitle(instructions)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setView(fragment)
                .create();
    }
}