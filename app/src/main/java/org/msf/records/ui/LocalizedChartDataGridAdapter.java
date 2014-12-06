package org.msf.records.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.chrono.ISOChronology;
import org.msf.records.R;
import org.msf.records.net.model.Concept;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.widget.DataGridAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import static org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;

/**
 * Attach the local cache of chart data into the necessary view for the chart history.
 */
public class LocalizedChartDataGridAdapter implements DataGridAdapter {

    private static class Row {

        private final String name;
        private final HashMap<String, String> datesToValues = new HashMap<>();
//        private final HashSet<String> dates = new HashSet<>();

        private Row(String name) {
            this.name = name;
        }
    }

    private final LayoutInflater mLayoutInflater;
    private final LocalDate today;
    private final ArrayList<Row> rows = new ArrayList<>();
    private final ArrayList<String> columnHeaders = new ArrayList<>();

    public LocalizedChartDataGridAdapter(Context context,
                                         ArrayList<LocalizedObservation> observations,
                                         LayoutInflater layoutInflater) {
        this.mLayoutInflater = layoutInflater;
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
            if (LocalizedChartHelper.UNKNOWN_VALUE.equals(ob.value)) {
                // This gives no extra information, ignore it.
                continue;
            }
            if (row == null || !ob.conceptName.equals(row.name)) {
                row = new Row(ob.conceptName);
                rows.add(row);
                if (Concept.DIARRHEA_UUID.equals(ob.conceptUuid)
                        || Concept.VOMITING_UUID.equals(ob.conceptUuid)) {
                    // TODO(nfortescue): this should really look up the localized values from the concept database
                    // otherwise the strings could be inconsistent with the form
                    severeRow = new Row(ob.conceptName + " (" + context.getResources().getString(R.string.severe_symptom) + ")");
                    moderateRow = new Row(ob.conceptName + " (" + context.getResources().getString(R.string.moderate_symptom) + ")");
                    mildRow = new Row(ob.conceptName + " (" + context.getResources().getString(R.string.mild_symptom) + ")");
                    extraRows.add(severeRow);
                    extraRows.add(moderateRow);
                    extraRows.add(mildRow);
                } else {
                    mildRow = null;
                    moderateRow = null;
                    severeRow = null;
                }
            }
            DateTime d = new DateTime(ob.encounterTimeMillis, chronology);
            LocalDate localDate = d.toLocalDate();
            String amKey = toAmKey(todayString, localDate);
            String pmKey = toPmKey(amKey); // this is never displayed to the user
            String dateKey = d.getHourOfDay() < 12 ? amKey : pmKey;
            days.add(localDate);
            // Only display dots for positive symptoms.
            if (!LocalizedChartHelper.NO_SYMPTOM_VALUES.contains(ob.value)) {
                row.datesToValues.put(dateKey, ob.value);
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

        rows.addAll(extraRows);
        // If there are no observations, put some known rows to make it clearer what is being
        // displayed.
        if (rows.isEmpty()) {
            ArrayList<LocalizedObservation> emptyChart = LocalizedChartHelper.getEmptyChart(
                    context.getContentResolver(),
                    LocalizedChartHelper.ENGLISH_LOCALE);
            for (LocalizedObservation ob : emptyChart) {
                rows.add(new Row(ob.conceptName));
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
    public View getRowHeader(int row, View convertView, ViewGroup parent) {
        View view = mLayoutInflater.inflate(
                R.layout.data_grid_row_header_chart, null /*root*/);
        TextView textView =
                (TextView) view.findViewById(R.id.data_grid_header_text);
        textView.setText(rows.get(row).name);

        textView.setBackgroundColor((row % 2 == 0) ? 0xffe1e1e1 : 0xffcccccc);

        return view;
    }

    @Override
    public View getColumnHeader(int column, View convertView, ViewGroup parent) {
        View view = mLayoutInflater.inflate(
                R.layout.data_grid_column_header_chart, null /*root*/);
        TextView textView =
                (TextView) view.findViewById(R.id.data_grid_header_text);
        textView.setText(columnHeaders.get(column));
        return view;
    }

    @Override
    public View getCell(int rowIndex, int columnIndex, View convertView, ViewGroup parent) {
        Row rowData = rows.get(rowIndex);
        View view = mLayoutInflater.inflate(
                R.layout.data_grid_cell_chart_text, null /*root*/);
        String dateKey = columnHeaders.get(columnIndex);

        View imageView = view.findViewById(R.id.data_grid_cell_chart_image);

        if (rowData.name.startsWith("Temperature")) {
            String temperatureString = rowData.datesToValues.get(dateKey);
            TextView textView = ((TextView) view.findViewById(R.id.data_grid_cell_chart_text));
            if (temperatureString != null) {
                double temperature = Double.parseDouble(temperatureString);
                textView.setText(String.format("%.1f", temperature));
                if (temperature < 37.5) {
                    imageView.setBackgroundResource(R.drawable.chart_cell_good);
                } else {
                    imageView.setBackgroundResource(R.drawable.chart_cell_bad);
                }
            }
        } else {
            boolean isActive = rowData.datesToValues.containsKey(dateKey);
            if (isActive) {
                imageView.setBackgroundResource(R.drawable.chart_cell_active);
            }
        }

        ((ViewGroup) view).getChildAt(0)
                .setBackgroundColor((rowIndex % 2 == 0) ? 0xffe1e1e1 : 0xffcccccc);

        return view;
    }
}
