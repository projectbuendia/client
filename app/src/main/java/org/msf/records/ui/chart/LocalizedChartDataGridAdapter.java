package org.msf.records.ui.chart;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.chrono.ISOChronology;
import org.msf.records.R;
import org.msf.records.net.model.Concept;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;
import org.msf.records.utils.Logger;
import org.msf.records.widget.DataGridAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import javax.annotation.Nullable;

/**
 * Attach the local cache of chart data into the necessary view for the chart history.
 */
final class LocalizedChartDataGridAdapter implements DataGridAdapter {

    private static final Logger LOG = Logger.create();

    private static final String EMPTY_STRING = "";
    private static final View.OnClickListener notesOnClickListener =
            new View.OnClickListener() {
                @Override public void onClick(View view) {
                    new AlertDialog.Builder(view.getContext())
                            .setMessage((String)view.getTag())
                            .show();
                }
            };
    private final Drawable backgroundLight;
    private final Drawable backgroundDark;

    private static class Row {
        private final String mConceptUuid;
        private final String mName;
        private final HashMap<String, String> datesToValues = new HashMap<>();

        private Row(String conceptUuid, String name) {
            mConceptUuid = conceptUuid;
            mName = name;
        }
    }

    private final LayoutInflater mLayoutInflater;
    private final LocalDate today;
    private final List<Row> rows = new ArrayList<>();
    private final List<String> columnHeaders = new ArrayList<>();

    public LocalizedChartDataGridAdapter(Context context,
                                         List<LocalizedObservation> observations,
                                         LayoutInflater layoutInflater) {
        Context context1 = context;
        LocalizedChartHelper localizedChartHelper = new LocalizedChartHelper(context.getContentResolver());
        mLayoutInflater = layoutInflater;
        Resources resources = context.getResources();
        this.backgroundLight = resources.getDrawable(R.drawable.chart_grid_background_light);
        this.backgroundDark = resources.getDrawable(R.drawable.chart_grid_background_dark);


        Row row = null;
        TreeSet<LocalDate> days = new TreeSet<>();
        ISOChronology chronology = ISOChronology.getInstance(DateTimeZone.getDefault());
        final String todayString = context.getResources().getString(R.string.today);
        today = new LocalDate(chronology);

        // Certain observations (specifically Diarrhea detail and vomiting detail) need to be
        // expanded into multiple rows at the end of the chart. Store them, then add them to the
        // rows at the end. This is known to be ugly and hacky.
        Row mildRow = null;
        Row moderateRow = null;
        Row severeRow = null;
        ArrayList<Row> extraRows = new ArrayList<>();

        for (LocalizedObservation ob : observations) {
            // Observations come through ordered by the chart row, then the observation time, so we
            // want to maintain that order.
            if (row == null || !ob.conceptName.equals(row.mName)) {
                row = new Row(ob.conceptUuid, ob.conceptName);
                rows.add(row);
                if (Concept.DIARRHEA_UUID.equals(ob.conceptUuid)
                        || Concept.VOMITING_UUID.equals(ob.conceptUuid)
                        || Concept.PAIN_UUID.equals(ob.conceptUuid)) {
                    // TODO(nfortescue): this should really look up the localized values from the concept database
                    // otherwise the strings could be inconsistent with the form
                    severeRow = new Row(null, ob.conceptName + " (" + context.getResources().getString(R.string.severe_symptom) + ")");
                    moderateRow = new Row(null, ob.conceptName + " (" + context.getResources().getString(R.string.moderate_symptom) + ")");
                    mildRow = new Row(null, ob.conceptName + " (" + context.getResources().getString(R.string.mild_symptom) + ")");
                    extraRows.add(severeRow);
                    extraRows.add(moderateRow);
                    extraRows.add(mildRow);
                } else {
                    mildRow = null;
                    moderateRow = null;
                    severeRow = null;
                }
            }

            if (ob.value == null || LocalizedChartHelper.UNKNOWN_VALUE.equals(ob.value)) {
                // Don't display any dots or handle dates if there are no positive observations.
                continue;
            }

            DateTime d = new DateTime(ob.encounterTimeMillis, chronology);
            LocalDate localDate = d.toLocalDate();
            String amKey = toAmKey(todayString, localDate);
            String pmKey = toPmKey(amKey); // this is never displayed to the user
            String dateKey = d.getHourOfDay() < 12 ? amKey : pmKey;
            days.add(localDate);

            // Only display dots for positive symptoms.
            if (!LocalizedChartHelper.NO_SYMPTOM_VALUES.contains(ob.value)) {
                // For notes, perform a concatenation rather than overwriting.
                if (Concept.NOTES_UUID.equals(ob.conceptUuid)) {
                    if (row.datesToValues.containsKey(dateKey)) {
                        row.datesToValues.put(
                                dateKey, row.datesToValues.get(dateKey) + '\n' + ob.value);
                    } else {
                        row.datesToValues.put(dateKey, ob.value);
                    }
                } else {
                    row.datesToValues.put(dateKey, ob.value);
                }
                // Do an ugly expansion into the extra rows.
                if (Concept.MILD_UUID.equals(ob.value) && mildRow != null) {
                    mildRow.datesToValues.put(dateKey, ob.value);
                }
                if (Concept.MODERATE_UUID.equals(ob.value) && moderateRow != null) {
                    moderateRow.datesToValues.put(dateKey, ob.value);
                }
                if (Concept.SEVERE_UUID.equals(ob.value) && severeRow != null) {
                    severeRow.datesToValues.put(dateKey, ob.value);
                }
            }
        }
        if (days.isEmpty()) {
            // Just put today, with nothing there.
            String am = toAmKey(todayString, today);
            columnHeaders.add(am);
            columnHeaders.add(toPmKey(am));
        } else {
            // Fill in all the columns between start and end.
            for (LocalDate d = days.first(); !d.isAfter(days.last()); d = d.plus(Days.ONE)) {
                String amKey = toAmKey(todayString, d);
                String pmKey = toPmKey(amKey); // this is never displayed to the user
                columnHeaders.add(amKey);
                columnHeaders.add(pmKey);
            }
        }

        rows.addAll(rows.size() - 1, extraRows);
        // If there are no observations, put some known rows to make it clearer what is being
        // displayed.
        if (rows.isEmpty()) {
            List<LocalizedObservation> emptyChart =
                    localizedChartHelper.getEmptyChart(LocalizedChartHelper.ENGLISH_LOCALE);
            for (LocalizedObservation ob : emptyChart) {
                rows.add(new Row(ob.conceptUuid, ob.conceptName));
            }
        }
    }

    private String toAmKey(String todayString, LocalDate localDate) {
        String amKey;
        int daysDiff = Days.daysBetween(localDate, today).getDays();
        if (daysDiff == 0) {
            amKey = todayString;
        } else {
            amKey = "-" + daysDiff + " Day";
        }
        return amKey;
    }

    private String toPmKey(String amKey) {
        return amKey + " PM";
    }

    @Override
    public int getRowCount() {
      return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnHeaders.size();
    }

    @Override
    public void fillRowHeader(int row, View view, TextView textView) {
        textView.setText(rows.get(row).mName);
    }

    @Override
    public View getRowHeader(int row, View convertView, ViewGroup parent) {
        View view = mLayoutInflater.inflate(
                R.layout.data_grid_row_header_chart, null /*root*/);
        TextView textView = (TextView) view.findViewById(R.id.data_grid_header_text);
        setCellBackgroundForViewType(textView, row % 2);
        fillRowHeader(row, view, textView);
        return view;
    }

    @Override
    public void setCellBackgroundForViewType(View view, int viewType) {
        view.setBackground(viewType == 0 ? backgroundLight : backgroundDark);
    }

    @Override
    public View getColumnHeader(int column, View convertView, ViewGroup parent) {
        TextView textView = (TextView) mLayoutInflater.inflate(
                R.layout.data_grid_column_header_chart, null /*root*/);
        fillColumnHeader(column, textView);
        return textView;
    }

    @Override
    public void fillColumnHeader(int column, TextView textView) {
        textView.setText(columnHeaders.get(column));
    }

    @Override
    public View getCell(int rowIndex, int columnIndex, View convertView, ViewGroup parent) {
        View view = mLayoutInflater.inflate(
                R.layout.data_grid_cell_chart_text, null /*root*/);
        setCellBackgroundForViewType(view, rowIndex % 2);
        ViewStub viewStub = (ViewStub) view.findViewById(R.id.data_grid_cell_chart_viewstub);
        fillCell(rowIndex, columnIndex, view, viewStub, null);
        return view;
    }

    @Override
    public @Nullable TextView fillCell(
            int rowIndex, int columnIndex, View view,
            @Nullable ViewStub viewStub, @Nullable TextView textView) {
        if (viewStub == null && textView == null) {
            throw new IllegalArgumentException("Either viewStub or textView have to be set.");
        }
        final Row rowData = rows.get(rowIndex);
        final String dateKey = columnHeaders.get(columnIndex);
        String text = EMPTY_STRING;
        String textViewTag = null;
        int backgroundResource = 0;
        View.OnClickListener onClickListener = null;

        String conceptUuid = rowData.mConceptUuid == null ? "" : rowData.mConceptUuid;
        switch (conceptUuid) {
            case Concept.TEMPERATURE_UUID:
                String temperatureString = rowData.datesToValues.get(dateKey);
                if (temperatureString != null) {
                    try {
                        double temperature = Double.parseDouble(temperatureString);
                        text = String.format(Locale.US, "%.1f", temperature);
                        if (temperature <= 37.5) {
                            backgroundResource = R.drawable.chart_cell_good;
                        } else {
                            backgroundResource = R.drawable.chart_cell_bad;
                        }
                    } catch (NumberFormatException e) {
                        LOG.w(e, "Temperature format was invalid");
                    }
                }
                break;
            case Concept.NOTES_UUID: {
                boolean isActive = rowData.datesToValues.containsKey(dateKey);
                if (isActive) {
                    backgroundResource = R.drawable.chart_cell_active_pressable;
                    textViewTag = rowData.datesToValues.get(dateKey);
                    onClickListener = notesOnClickListener;
                }
                break;
            }
            default: {
                boolean isActive = rowData.datesToValues.containsKey(dateKey);
                if (isActive) {
                    backgroundResource = R.drawable.chart_cell_active;
                }
                break;
            }
        }

        if (textView == null && backgroundResource != 0) {
            // We need the textView, so inflate it.
            textView = (TextView) viewStub.inflate();
        }
        if (textView != null) {
            textView.setText(text);
            textView.setTag(textViewTag);
            textView.setBackgroundResource(backgroundResource);
            textView.setOnClickListener(onClickListener);
        }
        return textView;
    }
}
