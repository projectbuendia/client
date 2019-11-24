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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.OrderAddRequestedEvent;
import org.projectbuendia.client.events.actions.OrderDeleteRequestedEvent;
import org.projectbuendia.client.events.actions.OrderStopRequestedEvent;
import org.projectbuendia.client.models.Catalog.Category;
import org.projectbuendia.client.models.Catalog.DosingType;
import org.projectbuendia.client.models.Catalog.Format;
import org.projectbuendia.client.models.Catalog.Route;
import org.projectbuendia.client.models.CatalogIndex;
import org.projectbuendia.client.models.MsfCatalog;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.ui.AutocompleteAdapter;
import org.projectbuendia.client.ui.EditTextWatcher;
import org.projectbuendia.client.utils.Utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.models.Catalog.Drug;
import static org.projectbuendia.client.utils.Utils.DateStyle.SENTENCE_MONTH_DAY;
import static org.projectbuendia.client.utils.Utils.eq;

/** A DialogFragment for creating or editing a treatment order. */
public class OrderDialogFragment extends BaseDialogFragment<OrderDialogFragment, OrderDialogFragment.Args> {
    public static final int MAX_FREQUENCY = 24;  // maximum 24 times per day
    public static final int MAX_DURATION_DAYS = 30;  // maximum 30 days

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
        RadioGroup category = u.findView(R.id.order_category);

        AutoCompleteTextView drug = u.findView(R.id.order_drug);
        Spinner format = u.findView(R.id.order_format);
        TableRow dosageRow = u.findView(R.id.dosage_row);
        EditText dosage = u.findView(R.id.order_dosage);
        TextView dosageUnit = u.findView(R.id.order_dosage_unit);
        Spinner route = u.findView(R.id.order_route);
        TableRow quantityOverDurationRow = u.findView(R.id.quantity_over_duration_row);
        EditText quantity = u.findView(R.id.order_quantity);
        TextView quantityUnit = u.findView(R.id.order_quantity_unit);
        EditText duration = u.findView(R.id.order_duration);
        TextView durationUnit = u.findView(R.id.order_duration_unit);

        RadioGroup isSeries = u.findView(R.id.order_is_series);
        RadioButton unary = u.findView(R.id.order_unary);
        RadioButton series = u.findView(R.id.order_series);
        TableRow frequencyRow = u.findView(R.id.frequency_row);
        EditText frequency = u.findView(R.id.order_frequency);
        TextView frequencyUnit = u.findView(R.id.order_frequency_unit);
        TableRow seriesLengthRow = u.findView(R.id.series_length_row);
        EditText seriesLength = u.findView(R.id.order_series_length);
        TextView seriesLengthUnit = u.findView(R.id.order_series_length_unit);
        TextView scheduleDescription = u.findView(R.id.order_schedule_description);
        EditText notes = u.findView(R.id.order_notes);
        Button stopNow = u.findView(R.id.order_stop_now);
        Button delete = u.findView(R.id.order_delete);
    }

    private Views v;
    private String orderUuid;
    private DateTime start;
    private AutocompleteAdapter autocompleter;

    private CatalogIndex index;

    /** The category for which the drug list and route list are configured. */
    private Category activeCategory = Category.UNSPECIFIED;

    /** The drug for which the format list is configured. */
    private Drug activeDrug = Drug.UNSPECIFIED;

    /* The format for which the dosing unit is configured. */
    private Format activeFormat = Format.UNSPECIFIED;

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

        // Attach the MSF catalog to the UI.
        index = MsfCatalog.INDEX;
        u.findView(R.id.oral_category).setTag(MsfCatalog.ORAL);
        u.findView(R.id.injectable_category).setTag(MsfCatalog.INJECTABLE);
        u.findView(R.id.infusible_category).setTag(MsfCatalog.INFUSIBLE);
        u.findView(R.id.external_category).setTag(MsfCatalog.EXTERNAL);
        u.findView(R.id.vaccine_category).setTag(MsfCatalog.VACCINE);

        orderUuid = args.order != null ? args.order.uuid : null;
        start = args.order != null ? args.order.start : args.now;

        dialog.setTitle(args.order == null ? R.string.title_new_order : R.string.title_edit_order);
        if (args.order != null) populateFields();
        updateUi();
        initDrugAutocompletion();
        addListeners();
    }

    private void initDrugAutocompletion() {
        addClearButton(v.drug, R.drawable.abc_ic_clear_mtrl_alpha);

        autocompleter = new AutocompleteAdapter(
            getActivity(), R.layout.captioned_item, index);
        v.drug.setAdapter(autocompleter);
        v.drug.setThreshold(1);

        // After the dialog has been laid out and positioned, we can figure out
        // how to position and size the autocompletion dropdown.
        v.drug.getViewTreeObserver().addOnPreDrawListener(() -> {
            adjustDropDownSize(v.drug, ITEM_HORIZONTAL_PADDING);
            return true;
        });

        // Open the keyboard, ready to type into the drug field.
        // TODO(ping): Figure out why this doesn't open the keyboard.
        v.drug.requestFocus();
    }

    private void addListeners() {
        v.category.setOnCheckedChangeListener((v, id) -> onCategorySelected());
        v.drug.setOnItemClickListener(
            (parent, view, pos, id) -> onDrugSelected((Drug) autocompleter.getItem(pos)));
        v.format.setOnItemClickListener(
            (parent, view, pos, id) -> onFormatSelected((Format) v.format.getSelectedItem()));
        v.isSeries.setOnCheckedChangeListener((v, id) -> updateUi());
        new EditTextWatcher(
            v.dosage, v.quantity, v.duration, v.frequency, v.seriesLength
        ).onChange(() -> updateUi());
        v.stopNow.setOnClickListener(view -> onStopNow());
        v.delete.setOnClickListener(view -> onDelete());
    }

    private void setSpinnerSelection(Spinner spinner, Object item) {
        for (int pos = 0; pos < spinner.getCount(); pos++) {
            if (eq(spinner.getItemAtPosition(pos), item)) {
                spinner.setSelection(pos);
                break;
            }
        }
    }

    private void populateFields() {
        v.drug.setText(args.order.instructions.medication);
        setSpinnerSelection(v.format, index.getFormat(args.order.instructions.formatCode));
        setSpinnerSelection(v.route, index.getRoute(args.order.instructions.route));
        v.dosage.setText(args.order.instructions.dosage);
        v.quantity.setText("" + args.order.instructions.volumeMilliliters);
        v.duration.setText("" + args.order.instructions.infusionHours);

        v.unary.setChecked(!args.order.isSeries());
        v.series.setChecked(args.order.isSeries());

        int frequency = args.order.instructions.frequency;
        v.frequency.setText(frequency > 0 ? "" + frequency : "");
        if (args.order.stop != null) {
            int days = Days.daysBetween(start.toLocalDate(), args.order.stop.toLocalDate()).getDays();
            if (days > 0) v.seriesLength.setText("" + days);
        }
        v.notes.setText(args.order.instructions.notes);
    }

    @Override protected void onSubmit() {
        String drug = activeDrug.code;
        String format = activeFormat.code;
        String dosage = Utils.getText(v.dosage);
        String route = ((Route) v.route.getSelectedItem()).code;
        double volume = Utils.getDouble(v.quantity, 0);
        double infusionHours = Utils.getDouble(v.duration, 0);
        int frequency = Utils.getInt(v.frequency, 0);
        String notes = Utils.getText(v.notes);

        Order.Instructions instructions = null;
        switch (activeCategory.dosingType) {
            case QUANTITY:
                instructions = new Order.Instructions(
                    drug, format, dosage, route, frequency, notes);
                break;
            case QUANTITY_OVER_DURATION:
                instructions = new Order.Instructions(
                    drug, format, volume, infusionHours, frequency, notes);
                break;
        }

        int seriesLengthDays = Utils.getInt(v.seriesLength, -1);
        boolean valid = true;
        if (drug.isEmpty()) {
            setError(v.drug, R.string.enter_medication);
            valid = false;
        }
        if (frequency > MAX_FREQUENCY) {
            setError(v.frequency, R.string.order_cannot_exceed_n_times_per_day, MAX_FREQUENCY);
            valid = false;
        }
        if (seriesLengthDays != -1) {
            if (seriesLengthDays == 0) {
                setError(v.seriesLength, R.string.order_give_for_days_cannot_be_zero);
                valid = false;
            }
            if (seriesLengthDays > MAX_DURATION_DAYS) {
                setError(v.seriesLength, R.string.order_cannot_exceed_n_days, MAX_DURATION_DAYS);
                valid = false;
            }
            if (start.plusDays(seriesLengthDays).isBefore(args.now)) {
                setError(v.seriesLength, R.string.order_cannot_stop_in_past);
                valid = false;
            }
        }
        Utils.logUserAction("order_submitted",
            "valid", "" + valid,
            "uuid", orderUuid,
            "drug", drug,
            "format", format,
            "dosage", dosage,
            "route", route,
            "volume", "" + volume,
            "infusionHours", "" + infusionHours,
            "frequency", "" + frequency,
            "seriesLengthDays", "" + seriesLengthDays,
            "notes", notes);
        if (!valid) return;

        dialog.dismiss();

        // Post an event that triggers the PatientChartController to save the order.
        EventBus.getDefault().post(new OrderAddRequestedEvent(
            orderUuid, args.patientUuid, Utils.getProviderUuid(),
            instructions, start, seriesLengthDays
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
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

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

    private static int getIntField(EditText field, int defaultValue) {
        return Utils.toIntOrDefault(field.getText().toString().trim(), defaultValue);
    }

    /** Updates elements and spinner contents when a category is selected. */
    private void onCategorySelected() {
        int categoryId = v.category.getCheckedRadioButtonId();
        Category category = categoryId == -1 ? Category.UNSPECIFIED :
            (Category) u.findView(categoryId).getTag();
        if (category == activeCategory) return;

        activeCategory = category;
        activeDrug = Drug.UNSPECIFIED;
        v.drug.setText("");
        populateFormatSpinner(new Format[] {Format.UNSPECIFIED});

        Utils.showIf(v.dosageRow, category.dosingType == DosingType.QUANTITY);
        Utils.showIf(v.quantityOverDurationRow, category.dosingType == DosingType.QUANTITY_OVER_DURATION);

        populateRouteSpinner(category.routes);
        Utils.showIf(v.route, category.routes.length > 0);
        v.route.setEnabled(activeFormat != null && category.routes.length > 1);

        clearDosage();
        clearSchedule();
        updateUi();
    }

    private void populateRouteSpinner(Route[] routes) {
        Route[] spinnerRoutes = routes;
        if (routes.length > 1) {
            // If there is more than one possible route, set a blank route as
            // the default so that the user has to make an explicit choice.
            spinnerRoutes = Utils.concat(Route.UNSPECIFIED, routes);
        }
        v.route.setAdapter(new ArrayAdapter<>(
            getActivity(), R.layout.custom_spinner_item, spinnerRoutes));
        if (routes.length > 0) v.route.setSelection(0);
    }

    private void onDrugSelected(Drug drug) {
        if (drug == activeDrug) return;

        activeDrug = drug;
        populateFormatSpinner(drug.formats);

        clearDosage();
        clearSchedule();
        updateUi();
    }

    private void populateFormatSpinner(Format[] formats) {
        // Always set a blank format as the default so that the user
        // has to make an explicit choice.
        Format[] spinnerFormats = Utils.concat(Format.UNSPECIFIED, formats);
        v.format.setAdapter(new ArrayAdapter<>(
            getActivity(), R.layout.custom_spinner_item, spinnerFormats));
        if (formats.length > 0) v.format.setSelection(0);
        activeFormat = Format.UNSPECIFIED;
    }

    private void onFormatSelected(Format format) {
        if (format == activeFormat) return;

        activeFormat = format;
        updateUi();
    }

    private void clearDosage() {
        v.dosage.setText("");
        v.quantity.setText("");
        v.duration.setText("");
    }

    private void clearSchedule() {
        v.unary.setChecked(true);
        v.series.setChecked(false);
        v.frequency.setText("");
        v.seriesLength.setText("");
        v.scheduleDescription.setText("");
    }

    /** Updates labels and disables or hides elements according to changes in input fields. */
    private void updateUi() {
        Format format = (Format) v.format.getSelectedItem();
        Locale locale = App.getSettings().getLocale();
        v.dosage.setEnabled(format != null);
        v.route.setEnabled(format != null && activeCategory.routes.length > 1);
        v.quantity.setEnabled(format != null);
        v.duration.setEnabled(format != null);
        v.isSeries.setEnabled(format != null);
        v.frequency.setEnabled(format != null);
        v.seriesLength.setEnabled(format != null);

        boolean isSeries = v.isSeries.getCheckedRadioButtonId() == R.id.order_series;
        Utils.showIf(v.frequencyRow, isSeries);
        Utils.showIf(v.seriesLength, isSeries);

        int dosage = Utils.getInt(v.dosage, 0);
        double hours = Utils.getDouble(v.duration, 0);
        int timesPerDay = Utils.getInt(v.frequency, 0);
        int days = Utils.getInt(v.seriesLength, 0);

        LocalDate startDay = start.toLocalDate();
        LocalDate stopDay = startDay.plusDays(days);

        v.dosageUnit.setText(
            format == null ? "" :
            (dosage == 1 ? format.dosageUnit.singular :
                format.dosageUnit.plural).get(locale));
        v.durationUnit.setText(
            hours == 1 ? R.string.order_hour : R.string.order_hours);
        v.frequencyUnit.setText(
            timesPerDay == 1 ? R.string.order_time_per_day : R.string.order_times_per_day);
        v.seriesLengthUnit.setText(
            days == 1 ? R.string.order_day : R.string.order_days);

        int doses = timesPerDay * days;
        String startDate = friendlyDateFormat(startDay);
        String stopDate = friendlyDateFormat(stopDay);

        if (args.order == null) {
            v.scheduleDescription.setText(
                timesPerDay == 0 || timesPerDay * days == 1 ?
                    u.str(R.string.order_one_dose_only) :
                days == 0 ?
                    u.str(R.string.order_start_now_indefinitely) :
                days == 1 ?
                    u.str(R.string.order_start_now_stop_after_doses, doses) :
                u.str(R.string.order_start_now_after_doses_stop_date, doses, stopDate)
            );
        } else if (args.stopped) {
            v.scheduleDescription.setText(
                timesPerDay == 0 || timesPerDay * days == 1 ?
                    u.str(R.string.order_one_dose_only_ordered_date, startDate) :
                u.str(R.string.order_started_date_stopped_date, startDate, stopDate)
            );
        } else {
            v.scheduleDescription.setText(
                timesPerDay == 0 || timesPerDay * days == 1 ?
                    u.str(R.string.order_one_dose_only_ordered_date, startDate) :
                days == 0 ?
                    u.str(R.string.order_started_date_indefinitely, startDate) :
                days == 1 ?
                    u.str(R.string.order_started_date_stop_after_doses, startDate, doses) :
                u.str(R.string.order_started_date_after_doses_stop_date, startDate, doses, stopDate)
            );
        }

        // If the order has stopped, you can't change the frequency or series length.
        Utils.setEnabled(v.frequency, !args.stopped);
        Utils.setEnabled(v.seriesLength, !args.stopped);

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
        textView.setDropDownWidth(v.drug.getWidth() + itemHorizontalPadding * 2);
    }

    private static Map<Integer, List<String>> getRoutesByCategoryId() {
        Map<Integer, List<String>> map = new HashMap<>();
        map.put(R.id.oral_category, Lists.newArrayList("PO"));
        map.put(R.id.injectable_category, Lists.newArrayList("IV", "SC", "IM"));
        return map;
    }
}
