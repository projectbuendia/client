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
import android.support.annotation.NonNull;
import android.text.method.KeyListener;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
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
import org.projectbuendia.client.events.actions.OrderDialogRequestedEvent;
import org.projectbuendia.client.events.actions.OrderStopRequestedEvent;
import org.projectbuendia.client.ui.AutocompleteAdapter;
import org.projectbuendia.client.ui.AutocompleteAdapter.CompletionAdapter;
import org.projectbuendia.client.ui.EditTextWatcher;
import org.projectbuendia.client.utils.Intl;
import org.projectbuendia.client.utils.Utils;
import org.projectbuendia.models.Catalog.Category;
import org.projectbuendia.models.Catalog.Drug;
import org.projectbuendia.models.Catalog.Format;
import org.projectbuendia.models.Catalog.Route;
import org.projectbuendia.models.CatalogIndex;
import org.projectbuendia.models.MsfCatalog;
import org.projectbuendia.models.Obs;
import org.projectbuendia.models.Order;
import org.projectbuendia.models.Order.Instructions;
import org.projectbuendia.models.Quantity;
import org.projectbuendia.models.Unit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.utils.Utils.DateStyle.SENTENCE_MONTH_DAY;
import static org.projectbuendia.client.utils.Utils.eq;

/** A DialogFragment for creating or editing a treatment order. */
public class OrderDialogFragment extends BaseDialogFragment<OrderDialogFragment, OrderDialogFragment.Args> {
    public static final int MAX_FREQUENCY = 24;  // maximum 24 times per day
    public static final int MAX_DURATION_DAYS = 30;  // maximum 30 days

    // This should match the left/right padding in the TextViews in captioned_item.xml.
    private static final int ITEM_HORIZONTAL_PADDING = 12;

    // This custom action mode callback disables the clipboard popup menu.
    private static final ActionMode.Callback PASTE_DISABLED = new ActionMode.Callback() {
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
        public boolean onCreateActionMode(ActionMode mode, Menu menu) { return false; }
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) { return false; }
        public void onDestroyActionMode(ActionMode mode) { }
    };

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
        TableRow continuousRow = u.findView(R.id.continuous_row);
        EditText amount = u.findView(R.id.order_amount);
        TextView amountUnit = u.findView(R.id.order_amount_unit);
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
        TableRow scheduleDescriptionRow = u.findView(R.id.schedule_description_row);
        EditText notes = u.findView(R.id.order_notes);
        Button stopNow = u.findView(R.id.order_stop_now);
        Button delete = u.findView(R.id.order_delete);
    }

    private Views v;
    private String orderUuid;
    private DateTime start;
    private AutocompleteAdapter autocompleter;
    private DrugCompletionAdapter adapter;
    private KeyListener drugKeyListener;
    private Map<String, Integer> buttonIdsByCategoryCode = new HashMap<>();

    private CatalogIndex index;

    /** The category for which the drug list and route list are configured. */
    private Category activeCategory = Category.UNSPECIFIED;

    /** The drug for which the format list is configured. */
    private Drug activeDrug = Drug.UNSPECIFIED;

    /* The format for which the dosing unit is configured. */
    private Format activeFormat = Format.UNSPECIFIED;

    /* The currently selected unit for dosage quantity. */
    private Unit activeUnit = Unit.UNSPECIFIED;

    /* The currently selected unit for administration duration. */
    private Unit activeDurationUnit = Unit.UNSPECIFIED;

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
        drugKeyListener = v.drug.getKeyListener();
        v.drug.setCustomSelectionActionModeCallback(PASTE_DISABLED);

        // Attach the MSF catalog to the UI.
        index = MsfCatalog.INDEX;
        attachCategory(R.id.oral_category, MsfCatalog.ORAL);
        attachCategory(R.id.injectable_category, MsfCatalog.INJECTABLE);
        attachCategory(R.id.infusible_category, MsfCatalog.PERFUSION);
        attachCategory(R.id.external_category, MsfCatalog.EXTERNAL);
        attachCategory(R.id.vaccine_category, MsfCatalog.VACCINE);

        orderUuid = args.order != null ? args.order.uuid : null;
        start = args.order != null ? args.order.start : args.now;

        dialog.setTitle(args.order == null ? R.string.title_new_order : R.string.title_edit_order);
        onCategorySelected();
        if (args.order != null) populateFields();
        updateUi();
        initDrugAutocompletion();
        addListeners();
    }

    private void attachCategory(int buttonId, Category category) {
        RadioButton button = u.findView(buttonId);
        button.setTag(category);
        buttonIdsByCategoryCode.put(category.code, buttonId);
    }

    private void initDrugAutocompletion() {
        adapter = new DrugCompletionAdapter(index);
        adapter.setCategory(activeCategory);
        autocompleter = new AutocompleteAdapter(
            getActivity(), R.layout.captioned_item, adapter);
        v.drug.setAdapter(autocompleter);
        v.drug.setThreshold(1);

        addClearButton(v.drug, R.drawable.abc_ic_clear_mtrl_alpha);

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
            (parent, view, pos, id) -> onDrugSelected(
                (Drug) autocompleter.getItem(pos)));

        v.format.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFormatSelected((Format) v.format.getSelectedItem());
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
                onFormatSelected(Format.UNSPECIFIED);
            }
        });

        View.OnClickListener onDosageUnitClicked = (view) -> selectUnit(
            R.string.select_dosage_unit, index.getDosageUnits(), (unit) -> {
                activeUnit = unit;
                updateUi();
            }
        );
        v.amountUnit.setOnClickListener(onDosageUnitClicked);
        v.dosageUnit.setOnClickListener(onDosageUnitClicked);
        v.durationUnit.setOnClickListener((view) -> selectUnit(
            R.string.select_duration_unit, index.getDurationUnits(), (unit) -> {
                activeDurationUnit = unit;
                updateUi();
            }
        ));

        v.isSeries.setOnCheckedChangeListener((v, id) -> onIsSeriesChanged());
        new EditTextWatcher(
            v.dosage, v.amount, v.duration, v.frequency, v.seriesLength
        ).onChange(() -> updateUi());

        v.stopNow.setOnClickListener(view -> onStopNow());
        v.delete.setOnClickListener(view -> onDelete());
    }

    interface OnUnitSelectedListener {
        void onUnitSelected(Unit unit);
    }

    private void selectUnit(int titleId, Unit[] units, OnUnitSelectedListener listener) {
        if (args.executed) return;  // Can't change dosage if already executed

        String[] labels = new String[units.length];
        int i = 0;
        for (Unit unit : units) {
            labels[i++] = getString(R.string.unit_with_abbreviation,
                App.localize(unit.abbr), App.localize(unit.plural));
        }

        new AlertDialog.Builder(getActivity())
            .setTitle(titleId)
            .setSingleChoiceItems(
                labels,
                Arrays.asList(units).indexOf(activeUnit),
                (dialog, index) -> {
                    listener.onUnitSelected(units[index]);
                    dialog.dismiss();
                })
            .show();
    }

    private void selectDurationUnit(View v) {
        if (args.executed) return;  // Can't change dosage if already executed

        Unit[] units = index.getDurationUnits();
        String[] labels = new String[units.length];

        int i = 0;
        for (Unit unit : units) {
            labels[i++] = getString(R.string.unit_with_abbreviation,
                App.localize(unit.abbr), App.localize(unit.plural));
        }

        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.select_dosage_unit)
            .setSingleChoiceItems(
                labels,
                Arrays.asList(units).indexOf(activeUnit),
                (dialog, index) -> {
                    activeUnit = units[index];
                    updateUi();
                    dialog.dismiss();
                })
            .show();
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
        Instructions instr = args.order.instructions;
        boolean isContinuous = args.order.isContinuous();
        boolean isSeries = args.order.isSeries();
        DateTime stop = args.order.stop;

        String formatCode = Utils.toNonnull(instr.code);
        String drugCode = formatCode.length() >= 8 ? formatCode.substring(0, 8) : "";
        String categoryCode = formatCode.length() >= 4 ? formatCode.substring(0, 4) : "";

        Integer buttonId = buttonIdsByCategoryCode.get(categoryCode);
        v.category.check(buttonId == null ? -1 : buttonId);
        onCategorySelected();

        Drug drug = index.getDrug(instr.code);
        onDrugSelected(drug);
        v.drug.setText(instr.getDrugName());

        Format format = index.getFormat(instr.code);
        setSpinnerSelection(v.format, format);
        onFormatSelected(format);

        setSpinnerSelection(v.route, index.getRoute(instr.route));
        if (instr.amount != null) {
            activeUnit = instr.amount.unit;
            if (isContinuous) {
                v.amount.setText(Utils.format(instr.amount.mag, 2));
                activeDurationUnit = instr.duration.unit;
                v.duration.setText(Utils.format(instr.duration.mag, 2));
            } else {
                v.dosage.setText(Utils.format(instr.amount.mag, 2));
            }
        }

        v.isSeries.check(isSeries ? v.series.getId() : v.unary.getId());
        v.frequency.setText(isSeries ? Utils.format(instr.frequency.mag, 2) : "");
        if (stop != null) {
            int days = Days.daysBetween(start.toLocalDate(), stop.toLocalDate()).getDays();
            if (days > 0) v.seriesLength.setText("" + days);
        }
        v.notes.setText(instr.notes);
    }

    @Override protected void onSubmit() {
        String code = activeFormat != Format.UNSPECIFIED ? activeFormat.code
            : activeDrug != Drug.UNSPECIFIED ? activeDrug.code
            : Utils.getText(v.drug);  // fall back to free text
        Quantity amount = Quantity.ZERO;
        Quantity duration = Quantity.ZERO;
        if (activeUnit != null) {
            amount = new Quantity(Utils.getDouble(v.dosage, 0), activeUnit);
            if (activeCategory.isContinuous) {
                amount = new Quantity(Utils.getDouble(v.amount, 0), activeUnit);
                duration = new Quantity(Utils.getDouble(v.duration, 0), activeDurationUnit);
            }
        }
        Route activeRoute = Utils.orDefault((Route) v.route.getSelectedItem(), Route.UNSPECIFIED);
        String route = activeRoute.code;
        boolean isSeries = v.isSeries.getCheckedRadioButtonId() == R.id.order_series;
        Quantity frequency = isSeries ?
            new Quantity(Utils.getDouble(v.frequency, 0), Unit.PER_DAY) : null;
        String notes = Utils.getText(v.notes);

        Instructions instructions = new Instructions(
            code, amount, duration, route, frequency, notes
        );

        int seriesLengthDays = Utils.getInt(v.seriesLength, -1);
        boolean valid = true;
        if (code.trim().isEmpty()) {
            setError(v.drug, R.string.enter_medication);
            valid = false;
        }
        if (valid && activeDrug != null) {
            if (activeCategory.isContinuous) {
                if (amount.mag == 0) {
                    setError(v.amount, R.string.enter_dosage);
                    valid = false;
                }
                if (valid && duration.mag == 0) {
                    setError(v.duration, R.string.enter_duration);
                    valid = false;
                }
            } else {
                if (amount.mag == 0) {
                    setError(v.dosage, R.string.enter_dosage);
                    valid = false;
                }
            }
        }
        if (valid && isSeries && frequency.mag == 0) {
            setError(v.frequency, R.string.enter_number);
            valid = false;
        }
        if (valid && isSeries && frequency.mag > MAX_FREQUENCY) {
            setError(v.frequency, R.string.order_cannot_exceed_n_times_per_day, MAX_FREQUENCY);
            valid = false;
        }
        if (valid && seriesLengthDays != -1) {
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
            "code", code,
            "amount", "" + amount,
            "duration", "" + duration,
            "route", route,
            "frequency", "" + frequency,
            "seriesLengthDays", "" + seriesLengthDays,
            "notes", notes);
        if (!valid) return;

        dialog.dismiss();

        // Facilitate entering several orders one after another.
        u.prompt(R.string.title_next_treatment, R.string.add_another_treatment,
            R.string.title_new_order, R.string.done, this::openNextOrder);

        // Post an event that triggers the PatientChartController to save the order.
        EventBus.getDefault().post(new OrderAddRequestedEvent(
            orderUuid, args.patientUuid, Utils.getProviderUuid(),
            instructions, start, seriesLengthDays > 0 ? seriesLengthDays : null
        ));
    }

    private void openNextOrder() {
        EventBus.getDefault().post(new OrderDialogRequestedEvent());
    }

    private void onStopNow() {
        dialog.dismiss();
        u.prompt(R.string.title_confirmation, R.string.confirm_order_stop, R.string.order_stop_now,
            () -> {
                Instructions instr = args.order.instructions;
                Utils.logUserAction("order_stop_requested",
                    "uuid", orderUuid,
                    "code", instr.code,
                    "amount", "" + instr.amount,
                    "duration", "" + instr.duration,
                    "route", instr.route,
                    "frequency", "" + instr.frequency,
                    "notes", instr.notes);
                EventBus.getDefault().post(new OrderStopRequestedEvent(orderUuid));
            }
        );
    }

    private void onDelete() {
        dialog.dismiss();
        u.prompt(R.string.title_confirmation, R.string.confirm_order_delete, R.string.delete,
            () -> {
                Instructions instr = args.order.instructions;
                Utils.logUserAction("order_delete_requested",
                    "uuid", orderUuid,
                    "code", instr.code,
                    "amount", "" + instr.amount,
                    "duration", "" + instr.duration,
                    "route", instr.route,
                    "frequency", "" + instr.frequency,
                    "notes", instr.notes);
                EventBus.getDefault().post(new OrderDeleteRequestedEvent(orderUuid));
            }
        );
    }

    /** Adds an "X" button to a text edit field. */
    private void addClearButton(final EditText view, int drawableId) {
        final Drawable icon = view.getResources().getDrawable(drawableId);
        icon.setColorFilter(0xff999999, PorterDuff.Mode.MULTIPLY);  // draw icon in grey
        final int iw = icon.getIntrinsicWidth();
        final int ih = icon.getIntrinsicHeight();
        icon.setBounds(0, 0, iw, ih);

        final Drawable cd[] = view.getCompoundDrawables();
        Runnable update = () -> {
            boolean show = view.getText().length() > 0;
            view.setCompoundDrawables(cd[0], cd[1], show ? icon : cd[2], cd[3]);
        };
        update.run();
        new EditTextWatcher(view).onChange(update);

        view.setMinimumHeight(view.getPaddingTop() + ih + view.getPaddingBottom());
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                if (x >= view.getWidth() - view.getPaddingRight() - iw && x < view.getWidth() &&
                    y >= 0 && y < view.getHeight()) {
                    view.setText("");
                    onDrugSelected(Drug.UNSPECIFIED);
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
        if (adapter != null) adapter.setCategory(category);
        activeDrug = Drug.UNSPECIFIED;
        v.drug.setText("");
        populateFormatSpinner(new Format[] {Format.UNSPECIFIED});

        Utils.showIf(v.dosageRow, !category.isContinuous);
        Utils.showIf(v.continuousRow, category.isContinuous);

        populateRouteSpinner(category.routes);
        Utils.showIf(v.route, category.routes.length > 0);
        Utils.setEnabled(v.route, activeFormat != null && category.routes.length > 1);

        clearDosage();
        clearSchedule();
        updateUi();
    }

    private void populateRouteSpinner(Route[] routes) {
        v.route.setAdapter(new ArrayAdapter<>(
            getActivity(), R.layout.spinner_item, routes));
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
        v.format.setAdapter(new ArrayAdapter<>(
            getActivity(), R.layout.spinner_item, formats));
        if (formats.length > 0) v.format.setSelection(0);
        activeFormat = Format.UNSPECIFIED;
    }

    private void onFormatSelected(Format format) {
        if (format == activeFormat) return;

        activeFormat = format;
        activeUnit = format.dosageUnit;
        activeDurationUnit = Unit.HOUR;
        updateUi();
        if (Utils.isVisible(v.dosageRow)) v.dosage.requestFocus();
        if (Utils.isVisible(v.continuousRow)) v.amount.requestFocus();
    }

    private void clearDosage() {
        v.dosage.setText("");
        v.dosage.setError(null);
        v.amount.setText("");
        v.amount.setError(null);
        v.duration.setText("");
        v.duration.setError(null);
        activeUnit = Unit.UNSPECIFIED;
        activeDurationUnit = Unit.UNSPECIFIED;
    }

    private void clearSchedule() {
        v.isSeries.check(v.unary.getId());
        v.frequency.setText("");
        v.frequency.setError(null);
        v.seriesLength.setText("");
        v.seriesLength.setError(null);
        v.scheduleDescription.setText("");
    }

    private void onIsSeriesChanged() {
        updateUi();
        if (Utils.isVisible(v.frequencyRow)) v.frequency.requestFocus();
    }

    /** Updates labels and disables or hides elements according to changes in input fields. */
    private void updateUi() {
        boolean drugSelected = !eq(activeDrug, Drug.UNSPECIFIED);
        boolean formatSelected = !eq(activeFormat, Format.UNSPECIFIED);

        // Disable text editing of the drug when a completion has been selected.
        v.drug.setKeyListener(drugSelected ? null : drugKeyListener);

        Utils.setEnabled(v.format, drugSelected);
        Utils.setEnabled(v.dosage, formatSelected);
        Utils.setEnabled(v.route, formatSelected);
        Utils.setEnabled(v.amount, formatSelected);
        Utils.setEnabled(v.duration, formatSelected);
        Utils.setChildrenEnabled(v.isSeries, formatSelected);
        Utils.setEnabled(v.frequency, formatSelected);
        Utils.setEnabled(v.seriesLength, formatSelected);

        boolean isSeries = v.isSeries.getCheckedRadioButtonId() == R.id.order_series;
        Utils.showIf(v.frequencyRow, isSeries);
        Utils.showIf(v.seriesLengthRow, isSeries);

        double dosage = Utils.getDouble(v.dosage, 0);
        double amount = Utils.getDouble(v.amount, 0);
        double duration = Utils.getDouble(v.duration, 0);
        int timesPerDay = Utils.getInt(v.frequency, 0);
        int days = Utils.getInt(v.seriesLength, 0);

        LocalDate startDay = start.toLocalDate();
        LocalDate stopDay = startDay.plusDays(days);

        Utils.showIf(v.dosageRow, !activeCategory.isContinuous && activeUnit != null);
        Utils.showIf(v.continuousRow, activeCategory.isContinuous && activeUnit != null);

        if (activeUnit != null) {
            if (activeCategory.isContinuous) {
                v.amountUnit.setText(getString(
                    R.string.order_volume_unit_in,
                    App.localize(activeUnit.forCount(amount))
                ));
                v.durationUnit.setText(App.localize(activeDurationUnit.forCount(duration)));
            } else {
                v.dosageUnit.setText(App.localize(activeUnit.forCount(dosage)));
            }
        }
        v.frequencyUnit.setText(App.localize(Unit.PER_DAY.forCount(timesPerDay)));
        v.seriesLengthUnit.setText(App.localize(Unit.DAY.forCount(days)));

        int doses = timesPerDay * days;
        String startDate = friendlyDateFormat(startDay);
        String stopDate = friendlyDateFormat(stopDay);

        if (args.order == null) {
            v.scheduleDescription.setText(
                !isSeries || timesPerDay == 0 || timesPerDay * days == 1 ?
                    u.str(R.string.order_one_dose_only_ordered_date, startDate) :
                days == 0 ?
                    u.str(R.string.order_start_now_indefinitely) :
                days == 1 ?
                    u.str(R.string.order_start_now_stop_after_n_doses, doses) :
                u.str(R.string.order_start_now_after_n_doses_stop_date, doses, stopDate)
            );
        } else {
            v.scheduleDescription.setText(
                !isSeries || timesPerDay == 0 || timesPerDay * days == 1 ?
                    u.str(R.string.order_one_dose_only_ordered_date, startDate) :
                days == 0 ?
                    u.str(R.string.order_started_date_indefinitely, startDate) :
                days == 1 ?
                    u.str(R.string.order_n_doses_in_one_day_ordered_date, doses, startDate) :
                u.str(R.string.order_started_date_after_n_doses_stop_date, startDate, doses, stopDate)
            );
        }

        // If already executed, all dosing fields are read-only.
        if (args.executed) {
            Utils.setChildrenEnabled(v.category, false);
            Utils.setEnabled(v.drug, false);
            Utils.setEnabled(v.format, false);
            Utils.setEnabled(v.dosage, false);
            Utils.setEnabled(v.route, false);
            Utils.setEnabled(v.amount, false);
            Utils.setEnabled(v.duration, false);
            (isSeries ? v.frequency : v.notes).requestFocus();  // focus on first enabled field
        }

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

    /** An adapter that provides completions for the AutocompleteAdapter. */
    class DrugCompletionAdapter implements CompletionAdapter<Drug> {
        private Map<String, String> targetsByDrugCode = new HashMap<>();
        private Category activeCategory = Category.UNSPECIFIED;

        public DrugCompletionAdapter(CatalogIndex index) {
            for (Category category : index.getCategories()) {
                for (Drug drug : category.drugs) {
                    targetsByDrugCode.put(drug.code, constructTarget(drug));
                }
            }
        }

        public void setCategory(Category category) {
            activeCategory = category;
        }

        public List<Drug> suggestCompletions(CharSequence constraint) {
            List<Drug> results = new ArrayList<>();
            String[] searchKeys = getSearchKeys(constraint);
            for (Drug drug : activeCategory.drugs) {
                if (isCompletionFor(searchKeys, drug)) {
                    results.add(drug);
                }
            }
            return results;
        }

        public void showInView(View view, Drug drug) {
            Utils.setText(view, R.id.label, App.localize(drug.name));
            String result = "";
            for (Intl caption : drug.captions) {
                if (!result.isEmpty()) result += ", ";
                result += App.localize(caption);
            }
            Utils.setText(view, R.id.caption, result);
        }

        public @NonNull String getCompletedText(Drug drug) {
            return App.localize(drug.name);
        }

        private String[] getSearchKeys(CharSequence constraint) {
            String[] searchKeys = normalize(constraint).trim().split(" ");
            for (int i = 0; i < searchKeys.length; i++) {
                searchKeys[i] = " " + searchKeys[i];
            }
            return searchKeys;
        }

        private boolean isCompletionFor(String[] searchKeys, Drug drug) {
            String target = targetsByDrugCode.get(drug.code);

            // Look for words matching the words in the input as prefixes.
            int score = 0;
            for (String searchKey : searchKeys) {
                score += target.contains(searchKey) ? 1 : 0;
            }
            if (score == searchKeys.length) return true;

            if (searchKeys.length == 1) {
                // Look for words matching the letters in the input as initials.
                score = 0;
                char[] initials = searchKeys[0].trim().toCharArray();
                for (char ch : initials) {
                    score += target.contains(" " + ch) ? 1 : 0;
                }
                if (score == initials.length) return true;
            }
            return false;
        }

        private String constructTarget(Drug drug) {
            String target = "";
            for (String localizedName : drug.name.getAll()) {
                target += " " + localizedName.toLowerCase();
            }
            for (Intl alias : drug.aliases) {
                for (String localizedAlias : alias.getAll()) {
                    target += " " + localizedAlias;
                }
            }
            String collapsed = target.replaceAll("[^a-z0-9]+", "");
            return normalize(" " + target + " " + collapsed + " ");
        }

        private String normalize(CharSequence name) {
            return name.toString().toLowerCase().replaceAll("[^a-z0-9]+", " ");
        }
    }
}
