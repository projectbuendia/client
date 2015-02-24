package org.msf.records.ui.chart;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
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
import org.msf.records.data.res.ResStatus;
import org.msf.records.model.Concepts;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;
import org.msf.records.utils.Logger;
import org.msf.records.widget.CellType;
import org.msf.records.widget.DataGridAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

/**
 * Attach the local cache of chart data into the necessary view for the chart history.
 */
final class LocalizedChartDataGridAdapter implements DataGridAdapter {

    public static final int TEXT_SIZE_LARGE = 26;
    public static final int TEXT_SIZE_NORMAL = 16;

    private static final String HEADER_TAG = "CELL_TYPE_HEADER";

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
    private final Drawable mBackgroundLight;
    private final Drawable mBackgroundDark;
    private final Drawable mRowHeaderBackgroundLight;
    private final Drawable mRowHeaderBackgroundDark;

    private static class Row {
        private final String mConceptUuid;
        private final String mName;
        private final HashMap<String, String> datesToValues = new HashMap<>();
        private final HashMap<String, String> mDatesToLocalizedValues = new HashMap<>();

        private Row(String conceptUuid, String name) {
            mConceptUuid = conceptUuid;
            mName = name;
        }
    }

    private final Set<String> mDatesWithAnyBleeding = new HashSet<>();

    private final LayoutInflater mLayoutInflater;
    private final LocalDate today;
    private final List<Row> rows = new ArrayList<>();
    private final List<String> columnHeaders = new ArrayList<>();
    private final Context mContext;

    public LocalizedChartDataGridAdapter(Context context,
                                         List<LocalizedObservation> observations,
                                         LocalDate admissionDate,
                                         LayoutInflater layoutInflater) {
        mContext = context;
        LocalizedChartHelper localizedChartHelper =
                new LocalizedChartHelper(context.getContentResolver());
        mLayoutInflater = layoutInflater;
        Resources resources = context.getResources();

        // Even though these Drawables are currently identical, referencing different drawables
        // for row headers and table cells ensures that RecyclerView does not improperly reuse
        // one as the other.
        this.mBackgroundLight = resources.getDrawable(R.drawable.chart_grid_background_light);
        this.mBackgroundDark = resources.getDrawable(R.drawable.chart_grid_background_dark);
        this.mRowHeaderBackgroundLight =
                resources.getDrawable(R.drawable.chart_grid_row_header_background_light);
        this.mRowHeaderBackgroundDark =
                resources.getDrawable(R.drawable.chart_grid_row_header_background_dark);


        Row row = null;
        TreeSet<LocalDate> days = new TreeSet<>();
        ISOChronology chronology = ISOChronology.getInstance(DateTimeZone.getDefault());
        final String todayString = context.getResources().getString(R.string.today);
        today = new LocalDate(chronology);
        // Today should always be shown in the chart, as well as any days between the last
        // observation and today.
        days.add(today);
        // Admission date should also always be present, as the left bound.
        if (admissionDate != null) {
            days.add(admissionDate);
        }
        for (LocalizedObservation ob : observations) {
            // Observations come through ordered by the chart row, then the observation time, so we
            // want to maintain that order.
            if (row == null || !ob.conceptName.equals(row.mName)) {
                row = new Row(ob.conceptUuid, ob.conceptName);
                rows.add(row);
            }

            if (ob.value == null || Concepts.UNKNOWN_UUID.equals(ob.value)
                    || LocalizedChartHelper.NO_SYMPTOM_VALUES.contains(ob.value)) {
                // Don't display anything in the cell if there are no positive observations.
                continue;
            }

            DateTime d = new DateTime(ob.encounterTimeMillis, chronology);
            LocalDate localDate = d.toLocalDate();
            days.add(localDate);
            String amKey = toAmKey(todayString, localDate, admissionDate);
            String pmKey = toPmKey(amKey); // this is never displayed to the user
            String dateKey = d.getHourOfDay() < 12 ? amKey : pmKey;

            // If this is any bleeding site, also show a dot in the "any bleeding" row.
            if (Concepts.BLEEDING_SITES_NAME.equals(ob.groupName)) {
                mDatesWithAnyBleeding.add(dateKey);
            }

            // For notes, perform a concatenation rather than overwriting.
            if (Concepts.NOTES_UUID.equals(ob.conceptUuid)) {
                String oldText = row.datesToValues.get(dateKey);
                String newText = (oldText == null ? "" : oldText + "\n—\n")
                        + d.toString("d MMM, hh:mm a\n") + ob.value;
                row.datesToValues.put(dateKey, newText);
            } else {
                row.datesToValues.put(dateKey, ob.value);
            }

            row.mDatesToLocalizedValues.put(dateKey, ob.localizedValue);
        }
        if (days.isEmpty()) {
            // Just put today, with nothing there.
            String am = toAmKey(todayString, today, today);
            columnHeaders.add(am);
            columnHeaders.add(toPmKey(am));
        } else {
            // Fill in all the columns between start and end.
            for (LocalDate d = days.first(); !d.isAfter(days.last()); d = d.plus(Days.ONE)) {
                String amKey = toAmKey(todayString, d, admissionDate);
                String pmKey = toPmKey(amKey); // this is never displayed to the user
                columnHeaders.add(amKey);
                columnHeaders.add(pmKey);
            }
        }

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

    private String toAmKey(String todayString, LocalDate localDate, LocalDate admissionDate) {
        // TODO: Localize.
        String localizedDateString = localDate.toString("d MMM");
        int daysDiff = Days.daysBetween(localDate, today).getDays();
        if (admissionDate == null) {
            if (daysDiff == 0) {
                return todayString + " (" + localizedDateString + ")";
            } else {
                return localizedDateString;
            }
        }

        String amKey = "";
        int day = Days.daysBetween(admissionDate, localDate).getDays() + 1;
        if (daysDiff == 0) {
            amKey = todayString + " (Day " + day + ")";
        } else if (day > 0) {
            amKey = "Day " + day;
        }
        amKey += "\n" + localizedDateString;
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
        View view;
        if (convertView == null || convertView.getTag() != HEADER_TAG) {
            view = mLayoutInflater.inflate(
                    R.layout.data_grid_row_header_chart, null /*root*/);
        } else {
            view = convertView;
        }
        TextView textView = (TextView) view.findViewById(R.id.data_grid_header_text);
        setCellBackgroundForViewType(textView, CellType.ROW_HEADER, row % 2);
        fillRowHeader(row, view, textView);
        view.setTag(HEADER_TAG);
        return view;
    }

    @Override
    public void setCellBackgroundForViewType(View view, CellType viewType, int rowType) {
        if (viewType == CellType.CELL) {
            view.setBackground(rowType % 2 == 0 ? mBackgroundLight : mBackgroundDark);
        } else {
            view.setBackground(
                    rowType % 2 == 0 ? mRowHeaderBackgroundLight : mRowHeaderBackgroundDark);
        }
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
        View view;
        if (convertView == null || convertView.getTag() == HEADER_TAG) {
            view = mLayoutInflater.inflate(
                    R.layout.data_grid_cell_chart_text, null /*root*/);
        } else {
            view = convertView;
        }
        setCellBackgroundForViewType(view, CellType.CELL, rowIndex % 2);
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
        int backgroundColor = 0;
        int textColor = 0;
        boolean useBigText = false;
        View.OnClickListener onClickListener = null;

        String conceptUuid = rowData.mConceptUuid == null ? "" : rowData.mConceptUuid;
        String value = rowData.datesToValues.get(dateKey);
        String localizedValue = rowData.mDatesToLocalizedValues.get(dateKey);

        // TODO: Proper localization.
        switch (conceptUuid) {
            case Concepts.TEMPERATURE_UUID:
                String temperatureString = rowData.datesToValues.get(dateKey);
                if (temperatureString != null) {
                    try {
                        double temperature = Double.parseDouble(temperatureString);
                        text = String.format(Locale.US, "%.1f", temperature);
                        textColor = Color.WHITE;
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
            case Concepts.GENERAL_CONDITION_UUID:
                String conditionUuid = rowData.datesToValues.get(dateKey);
                if (conditionUuid != null) {
                    ResStatus resStatus = Concepts.getResStatus(conditionUuid);
                    ResStatus.Resolved status = resStatus.resolve(mContext.getResources());
                    text = status.getShortDescription().toString();
                    textColor = status.getForegroundColor();
                    backgroundColor = status.getBackgroundColor();
                    useBigText = true;
                }
                break;
            // Concepts with a discrete count value.
            case Concepts.DIARRHEA_UUID:
            case Concepts.VOMITING_UUID:
                if (value != null) {
                    try {
                        int valueInt = Integer.parseInt(value);
                        text = String.format(Locale.US, "%d", valueInt);
                        textColor = Color.BLACK;
                        useBigText = true;
                    } catch (NumberFormatException e) {
                        LOG.w(e, "Format for concept %s was invalid", conceptUuid);
                    }
                }
                break;
            case Concepts.WEIGHT_UUID:
                String weightString = rowData.datesToValues.get(dateKey);
                if (weightString != null) {
                    try {
                        double weight = Double.parseDouble(weightString);
                        if (weight >= 10) {
                            text = String.format(Locale.US, "%d", (int) weight);
                        } else {
                            text = String.format(Locale.US, "%.1f", weight);
                        }
                        textColor = Color.BLACK;
                        // Three digits won't fit in the TextView with the larger font.
                        if (weight < 100) {
                            useBigText = true;
                        }
                    } catch (NumberFormatException e) {
                        LOG.w(e, "Weight format was invalid");
                    }
                }
                break;
            case Concepts.BLEEDING_UUID:
                if (rowData.datesToValues.containsKey(dateKey)
                        || mDatesWithAnyBleeding.contains(dateKey)) {
                    backgroundResource = R.drawable.chart_cell_active;
                }
                break;
            case Concepts.RESPONSIVENESS_UUID:
            case Concepts.MOBILITY_UUID:
            case Concepts.PAIN_UUID:
            case Concepts.WEAKNESS_UUID:
                // If there is a dot after the first one or two characters, use those characters
                // as the abbreviation for the table.  Otherwise just use the first two characters.
                if (localizedValue != null) {
                    int abbrevLength = localizedValue.indexOf('.');
                    if (abbrevLength < 1 || abbrevLength > 2) {
                        abbrevLength = 2;
                    }
                    text = localizedValue.substring(0, abbrevLength);
                    useBigText = true;
                }
                textColor = Concepts.SEVERE_UUID.equals(value)
                        ? Color.RED : Color.BLACK;
                break;
            case Concepts.NOTES_UUID: {
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

        if (textView == null) {
            // We need the textView, so inflate it.
            textView = (TextView) viewStub.inflate();
        }
        if (textView != null) {
            textView.setText(text);
            textView.setTag(textViewTag);
            textView.setBackgroundResource(backgroundResource);
            if (backgroundColor != 0) {
                textView.setBackgroundColor(backgroundColor);
            }
            if (textColor != 0) {
                textView.setTextColor(textColor);
            }
            if (useBigText) {
                textView.setTextSize(TEXT_SIZE_LARGE);
            } else {
                textView.setTextSize(TEXT_SIZE_NORMAL);
            }
            textView.setOnClickListener(onClickListener);
        }
        return textView;
    }
}
