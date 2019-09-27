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

import android.app.Dialog;
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
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.ObsDeleteRequestedEvent;
import org.projectbuendia.client.events.actions.OrderExecutionAddRequestedEvent;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.utils.Utils.DateStyle.HOUR_MINUTE;
import static org.projectbuendia.client.utils.Utils.DateStyle.SENTENCE_MONTH_DAY;

/** A {@link DialogFragment} for recording that an order was executed. */
public class OrderExecutionDialogFragment extends BaseDialogFragment<OrderExecutionDialogFragment, OrderExecutionDialogFragment.Args> {
    static class Args implements Serializable {
        DateTime now;
        Order order;
        LocalDate date;
        List<Obs> executions;
        boolean executable;
    }

    class Views {
        TextView orderDescription = u.findView(R.id.order_description);
        TextView orderStartTime = u.findView(R.id.order_start_time);
        TextView executionCount = u.findView(R.id.execution_count);
        ViewGroup executionList = u.findView(R.id.execution_list);
        ToggleButton execute = u.findView(R.id.execute_toggle);
        Button delete = u.findView(R.id.delete_button);
    }

    private Views v;
    private List<View> items;
    private View newItem;
    private Set<String> obsUuidsToDelete = new HashSet<>();

    /** Creates a new instance showing a list of executions in the order given. */
    public static OrderExecutionDialogFragment create(
        Order order, Interval interval, List<Obs> executions) {
        Args args = new Args();
        // To avoid the possibility of confusion when the dialog is opened just
        // before midnight, save the current time for use as the encounter time later.
        args.now = DateTime.now();
        args.order = order;
        args.date = interval.getStart().toLocalDate();
        args.executions = new ArrayList<>();
        for (Obs obs : executions) {
            if (interval.contains(obs.time)) args.executions.add(obs);
        }
        args.executable = interval.contains(args.now);
        return new OrderExecutionDialogFragment().withArgs(args);
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.order_execution_dialog_fragment);
    }

    protected void onOpen() {
        v = new Views();

        dialog.setTitle(getString(
            R.string.order_execution_title, Utils.format(args.date, SENTENCE_MONTH_DAY)));

        // Show what was ordered and when the order started.
        v.orderDescription.setText(Html.fromHtml(describeOrderHtml(args.order)));
        v.orderStartTime.setText(getString(
            R.string.order_started_datetime,
            Utils.format(args.order.start, Utils.DateStyle.SENTENCE_MONTH_DAY_HOUR_MINUTE)));

        // Populate the list of execution times with checkable items.
        Utils.showIf(v.executionList, args.executions.size() > 0 || args.executable);

        items = new ArrayList<>();
        for (Obs obs : args.executions) {
            View item = u.inflate(R.layout.checkable_item, v.executionList);
            u.setText(R.id.text, Utils.format(obs.time, HOUR_MINUTE));
            App.getUserManager().showChip(u.findView(R.id.user_initials), obs.providerUuid);
            item.setTag(obs);
            items.add(item);
            v.executionList.addView(item);

            final CheckBox checkbox = u.findView(R.id.checkbox);
            checkbox.setOnCheckedChangeListener((view, checked) -> updateUi());
            item.setOnClickListener(view -> {
                if (checkbox.isEnabled()) checkbox.setChecked(!checkbox.isChecked());
            });
        }
        if (args.executable) {
            newItem = u.inflate(R.layout.checkable_item, v.executionList);
            u.setText(R.id.text, Html.fromHtml(toBoldHtml(Utils.format(args.now, HOUR_MINUTE))));
            u.findView(R.id.checkbox).setVisibility(View.INVISIBLE);
            v.executionList.addView(newItem);
            newItem.setVisibility(View.INVISIBLE);
        }

        // Set up listeners.
        updateUi();
        v.execute.setOnCheckedChangeListener((button, checked) -> updateUi());
        v.delete.setOnClickListener(view -> deleteSelected());
    }

    /** Updates the UI to reflect the changes proposed by the user. */
    private void updateUi() {
        boolean executeNow = v.execute.isChecked();

        // Describe how many times the order was executed during the selected interval.
        int count = items.size() - obsUuidsToDelete.size() + (executeNow ? 1 : 0);
        boolean plural = count != 1;
        v.executionCount.setText(Html.fromHtml(getString(
            args.date.equals(LocalDate.now()) ?
                (plural ? R.string.order_execution_today_plural_html
                    : R.string.order_execution_today_singular_html) :
                (plural ? R.string.order_execution_historical_plural_html
                    : R.string.order_execution_historical_singular_html),
            count, Utils.format(args.date, SENTENCE_MONTH_DAY))));

        // Update the list of execution times.
        for (View item : items) {
            Obs obs = (Obs) item.getTag();
            if (obsUuidsToDelete.contains(obs.uuid)) {
                strikeCheckableItem(item);
            }
        }
        if (newItem != null) {
            newItem.setVisibility(executeNow ? View.VISIBLE : View.INVISIBLE);
        }

        boolean anyItemsChecked = false;
        for (View item : items) {
            CheckBox checkbox = item.findViewById(R.id.checkbox);
            if (checkbox.isChecked()) anyItemsChecked = true;
        }

        // To keep the dialog simple, we only show one button at a time (either
        // the Mark button or the Delete button).  To keep the dialog from
        // resizing, we always show one button.
        boolean showDeleteButton;
        if (anyItemsChecked || obsUuidsToDelete.size() > 0) {
            showDeleteButton = true;
        } else if (args.executable) {
            showDeleteButton = false;
        } else {
            showDeleteButton = true;
        }
        Utils.showIf(v.execute, !showDeleteButton);
        Utils.showIf(v.delete, showDeleteButton);

        // Special case: if neither button can ever be used, hide both.
        if (!args.executable && !Utils.hasItems(items)) {
            Utils.showIf(v.execute, false);
            Utils.showIf(v.delete, false);
        }

        // Light up the Delete button only if it has an effect.
        v.delete.setEnabled(anyItemsChecked);
        v.delete.setBackgroundColor(anyItemsChecked ? 0xffff6666 : 0xffcccccc);

        // If execution is requested, prevent marking anything for deletion;
        // otherwise just disable the checkboxes of items already marked for deletion.
        if (v.execute.isChecked()) {
            for (View item : items) item.findViewById(R.id.checkbox).setEnabled(false);
        } else {
            for (View item : items) {
                Obs obs = (Obs) item.getTag();
                item.findViewById(R.id.checkbox).setEnabled(
                    !obsUuidsToDelete.contains(obs.uuid));
            }
        }
    }

    /** Constructs an HTML description of the order. */
    private String describeOrderHtml(Order order) {
        String htmlDescription = toBoldHtml(getString(
            R.string.order_medication_route,
            order.instructions.medication,
            order.instructions.route
        ));
        String dosage = order.instructions.dosage;
        if (Utils.isBlank(dosage)) {
            dosage = u.str(R.string.order_unspecified_dosage);
        }
        htmlDescription += "<br>" + toHtml(getString(
            order.isSeries() ? R.string.order_dosage_series : R.string.order_dosage_unary,
            dosage, order.instructions.frequency
        ));
        if (!Utils.isBlank(order.instructions.notes)) {
            htmlDescription += "<br>" + toItalicHtml(order.instructions.notes);
        }
        return htmlDescription;
    }

    /** Marks the checked items for deletion. */
    private void deleteSelected() {
        for (View item : items) {
            Obs obs = (Obs) item.getTag();
            CheckBox checkbox = item.findViewById(R.id.checkbox);
            if (checkbox.isChecked()) {
                obsUuidsToDelete.add(obs.uuid);
                checkbox.setChecked(false);
            }
        }
        updateUi();
    }

    /** Applies the requested new execution or deletions. */
    @Override public void onSubmit() {
        if (v.execute.isChecked()) {
            Utils.logUserAction("order_execution_submitted",
                "orderUuid", args.order.uuid,
                "instructions", "" + args.order.instructions,
                "executionTime", "" + args.now);
            EventBus.getDefault().post(
                new OrderExecutionAddRequestedEvent(args.order.uuid, args.now));
        }

        List<Obs> executionsToDelete = new ArrayList<>();
        for (Obs obs : args.executions) {
            if (obsUuidsToDelete.contains(obs.uuid)) {
                executionsToDelete.add(obs);
            }
        }

        if (obsUuidsToDelete.size() > 0) {
            Utils.logUserAction("order_execution_deleted",
                "orderUuid", args.order.uuid,
                "obsUuids", Joiner.on(",").join(obsUuidsToDelete));
            EventBus.getDefault().post(
                new ObsDeleteRequestedEvent(executionsToDelete));
        }
        dialog.dismiss();
    }
}
