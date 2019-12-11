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
import org.projectbuendia.models.Catalog.Drug;
import org.projectbuendia.models.Catalog.Format;
import org.projectbuendia.models.MsfCatalog;
import org.projectbuendia.models.Obs;
import org.projectbuendia.models.Order;
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
        dialog.getButton(BUTTON_NEUTRAL).setText(R.string.delete_selected);
        u.show(dialog.getButton(BUTTON_NEUTRAL), args.executions.size() > 0);

        // Show what was ordered and when the order started.
        v.orderDescription.setText(Html.fromHtml(describeOrderHtml(args.order.instructions)));
        v.orderStartTime.setText(getString(
            R.string.order_started_datetime,
            Utils.format(args.order.start, Utils.DateStyle.SENTENCE_MONTH_DAY_HOUR_MINUTE)));

        // Populate the list of execution times with checkable items.
        Utils.showIf(v.executionList, args.executions.size() > 0 || args.executable);

        items = new ArrayList<>();
        for (Obs obs : args.executions) {
            View item = u.addInflated(R.layout.checkable_item, v.executionList);
            u.setText(R.id.text, Utils.format(obs.time, HOUR_MINUTE));
            App.getUserManager().showChip(u.findView(R.id.user_initials), obs.providerUuid);
            item.setTag(obs.uuid);
            items.add(item);

            final CheckBox checkbox = u.findView(R.id.checkbox);
            checkbox.setOnCheckedChangeListener((view, checked) -> updateUi());
            item.setOnClickListener(view -> {
                if (checkbox.isEnabled()) checkbox.setChecked(!checkbox.isChecked());
            });
        }
        if (args.executable) {
            newItem = u.addInflated(R.layout.checkable_item, v.executionList);
            u.setText(R.id.text, Html.fromHtml(toBoldHtml(Utils.format(args.now, HOUR_MINUTE))));
            String providerUuid = App.getUserManager().getActiveUser().getUuid();
            App.getUserManager().showChip(u.findView(R.id.user_initials), providerUuid);
            u.cloak(R.id.checkbox);
            u.show(R.id.arrow);
            u.cloak(newItem);
        }

        u.show(v.execute, args.executable);
        v.execute.setOnCheckedChangeListener((button, checked) -> updateUi());
        dialog.getButton(BUTTON_NEUTRAL).setOnClickListener(v -> deleteSelected());
        updateUi();
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
        boolean anyItemsChecked = false;
        for (View item : items) {
            CheckBox checkbox = item.findViewById(R.id.checkbox);
            if (checkbox.isChecked()) anyItemsChecked = true;
            if (obsUuidsToDelete.contains(item.getTag())) {
                strikeCheckableItem(item);
            }
        }
        if (newItem != null) {
            newItem.setVisibility(executeNow ? View.VISIBLE : View.INVISIBLE);
        }

        dialog.getButton(BUTTON_NEUTRAL).setEnabled(anyItemsChecked);
        v.execute.setEnabled(!anyItemsChecked && obsUuidsToDelete.isEmpty());

        // If execution is requested, prevent marking anything for deletion;
        // otherwise, disable the items that are marked for deletion.
        for (View item : items) {
            item.findViewById(R.id.checkbox).setEnabled(
                v.execute.isChecked() ? false : !obsUuidsToDelete.contains(item.getTag())
            );
        }
    }

    /** Constructs an HTML description of the order. */
    private String describeOrderHtml(Order.Instructions instr) {
        Drug drug = MsfCatalog.INDEX.getDrug(instr.code);
        Format format = MsfCatalog.INDEX.getFormat(instr.code);
        String dosage = instr.amount != null ? (
            instr.duration != null ? u.str(
                R.string.amount_in_duration,
                instr.amount.formatLong(2), instr.duration.formatLong(2)
            ) : instr.amount.formatLong(2)
        ) : u.str(R.string.order_unspecified_dosage);
        String htmlDescription = toBoldHtml(App.localize(drug.name))
            + "<br>"
            + (format != null ? toHtml(getString(R.string.order_execution_format,
                App.localize(format.description))
            ) : "")
            + "<br>"
            + toHtml(getString(R.string.order_execution_dosage,
                instr.isSeries()
                    ? getString(R.string.order_dosage_series, dosage, Utils.format(instr.frequency.mag, 2))
                    : getString(R.string.order_dosage_unary, dosage)
            ));
        if (!Utils.isBlank(instr.notes)) {
            htmlDescription += "<br>" + toItalicHtml(instr.notes);
        }
        return htmlDescription;
    }

    /** Marks the checked items for deletion. */
    private void deleteSelected() {
        for (View item : items) {
            CheckBox checkbox = item.findViewById(R.id.checkbox);
            if (checkbox.isChecked()) {
                obsUuidsToDelete.add((String) item.getTag());
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

        if (obsUuidsToDelete.size() > 0) {
            List<Obs> executionsToDelete = new ArrayList<>();
            for (Obs obs : args.executions) {
                if (obsUuidsToDelete.contains(obs.uuid)) {
                    executionsToDelete.add(obs);
                }
            }
            Utils.logUserAction("order_execution_deleted",
                "orderUuid", args.order.uuid,
                "obsUuids", Joiner.on(",").join(obsUuidsToDelete));
            EventBus.getDefault().post(
                new ObsDeleteRequestedEvent(executionsToDelete));
        }
        dialog.dismiss();
    }
}
