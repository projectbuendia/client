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

import org.joda.time.Chronology;
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
import org.msf.records.utils.date.Dates;
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

/** Adapter that populates a grid view with the local cache of a patient's chart history. */
final class LocalizedChartDataGridAdapter implements DataGridAdapter {

    public static final int TEXT_SIZE_LARGE = 26;
    public static final int TEXT_SIZE_NORMAL = 16;

    private static final String HEADER_TAG = "CELL_TYPE_HEADER";

    private static final Logger LOG = Logger.create();

    private static final String EMPTY_STRING = "";
    private static final View.OnClickListener sNotesOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(view.getContext())
                            .setMessage((String) view.getTag())
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
        private final HashMap<String, String> mColumnIdsToValues = new HashMap<>();
        private final HashMap<String, String> mColumnIdsToLocalizedValues = new HashMap<>();

        public Row(String conceptUuid, String name) {
            mConceptUuid = conceptUuid;
            mName = name;
        }
    }

    private final Set<String> mColumnIdsWithAnyBleeding = new HashSet<>();

    private final LayoutInflater mLayoutInflater;
    private final LocalDate mToday;
    private final List<Row> mRows = new ArrayList<>();
    private final List<String> mColumnIds = new ArrayList<>();
    private final Chronology mChronology = ISOChronology.getInstance(DateTimeZone.getDefault());
    private final Context mContext;
    private final LocalDate mAdmissionDate;
    private final LocalDate mFirstSymptomsDate;

    public LocalizedChartDataGridAdapter(Context context,
                                         List<LocalizedObservation> observations,
                                         LocalDate admissionDate,
                                         LocalDate firstSymptomsDate,
                                         LayoutInflater layoutInflater) {
        mContext = context;
        LocalizedChartHelper localizedChartHelper =
                new LocalizedChartHelper(context.getContentResolver());
        mLayoutInflater = layoutInflater;
        Resources resources = context.getResources();
        mAdmissionDate = admissionDate;
        mFirstSymptomsDate = firstSymptomsDate;

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
        mToday = LocalDate.now(mChronology);
        for (LocalizedObservation ob : observations) {
            // Observations come through ordered by the chart row, then the observation time, so we
            // want to maintain that order.
            if (row == null || !ob.conceptName.equals(row.mName)) {
                row = new Row(ob.conceptUuid, ob.conceptName);
                mRows.add(row);
            }

            if (ob.value == null || Concepts.UNKNOWN_UUID.equals(ob.value)
                    || LocalizedChartHelper.NO_SYMPTOM_VALUES.contains(ob.value)) {
                // Don't display anything in the cell if there are no positive observations.
                continue;
            }

            DateTime obsDateTime = new DateTime(ob.encounterTimeMillis, mChronology);
            String columnId = toColumnId(obsDateTime);
            days.add(obsDateTime.toLocalDate());

            LOG.v("Column: %s, Observation: %s", columnId, ob.toString());
            if (row.mColumnIdsToLocalizedValues.containsKey(columnId)) {
                LOG.v("Overriding previous observation with value: %s",
                        row.mColumnIdsToLocalizedValues.get(columnId));
            }

            // If this is any bleeding site, also show a dot in the "any bleeding" row.
            if (Concepts.BLEEDING_SITES_NAME.equals(ob.groupName)) {
                mColumnIdsWithAnyBleeding.add(columnId);
            }

            // For notes, perform a concatenation rather than overwriting.
            if (Concepts.NOTES_UUID.equals(ob.conceptUuid)) {
                String oldText = row.mColumnIdsToValues.get(columnId);
                String newText = (oldText == null ? "" : oldText + "\n—\n")
                        + Dates.toMediumString(obsDateTime) + "\n" + ob.value;
                row.mColumnIdsToValues.put(columnId, newText);
            } else {
                row.mColumnIdsToValues.put(columnId, ob.value);
            }

            row.mColumnIdsToLocalizedValues.put(columnId, ob.localizedValue);
        }

        // Create the list of all the columns to show.  Today and the admission date should
        // always be present, as well as any days between the last observation and today.
        days.add(mToday);
        if (admissionDate != null) {
            days.add(admissionDate);
        }
        LocalDate lastDay = days.last();
        for (LocalDate d = days.first(); !d.isAfter(lastDay); d = d.plus(Days.ONE)) {
            DateTime dayStart = d.toDateTimeAtStartOfDay();
            mColumnIds.add(toColumnId(dayStart));
            mColumnIds.add(toColumnId(dayStart.withHourOfDay(12)));
        }

        // If there are no observations, put some known rows to make it clearer what is being
        // displayed.
        if (mRows.isEmpty()) {
            List<LocalizedObservation> emptyChart =
                    localizedChartHelper.getEmptyChart(LocalizedChartHelper.ENGLISH_LOCALE);
            for (LocalizedObservation ob : emptyChart) {
                mRows.add(new Row(ob.conceptUuid, ob.conceptName));
            }
        }
    }

    // Determines the column in which a particular DateTime belongs, returning a sortable
    // string ID for the column.
    private String toColumnId(DateTime dateTime) {
        int hour = dateTime.getHourOfDay() >= 12 ? 12 : 0;  // choose am or pm column
        return dateTime.withTimeAtStartOfDay().withHourOfDay(hour).toString();
    }

    private String formatColumnHeader(String columnId) {
        DateTime dateTime = DateTime.parse(columnId).withChronology(mChronology);
        LocalDate date = dateTime.toLocalDate();
        Resources res = mContext.getResources();

        int admitDay = Dates.dayNumberSince(mAdmissionDate, date);
        String admitDayLabel = (admitDay >= 1) ? res.getString(R.string.day_n, admitDay) : "–";

        String dateString = date.toString("d MMM");
        String dateLabel = date.equals(mToday)
                ? res.getString(R.string.today_date, dateString) : dateString;

        // The column header has two lines of text: the first line gives the day number since
        // admission and the second line gives the calendar date.  Pending feedback from the field
        // on its importance, the symptom onset day number could also be shown in the column
        // header in a different colour.  This would be done by constructing HTML and using
        // Html.fromHtml() to produce a Spanned object that will be rendered by the TextView.
        return admitDayLabel + "\n" + dateLabel;
    }

    @Override
    public int getRowCount() {
        return mRows.size();
    }

    @Override
    public int getColumnCount() {
        return mColumnIds.size();
    }

    @Override
    public void fillRowHeader(int row, View view, TextView textView) {
        textView.setText(mRows.get(row).mName);
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
        textView.setText(formatColumnHeader(mColumnIds.get(column)));
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
    @Nullable
    public TextView fillCell(
            int rowIndex, int columnIndex, View view,
            @Nullable ViewStub viewStub, @Nullable TextView textView) {
        if (viewStub == null && textView == null) {
            throw new IllegalArgumentException("Either viewStub or textView have to be set.");
        }
        final Row rowData = mRows.get(rowIndex);
        final String columnId = mColumnIds.get(columnIndex);
        String text = EMPTY_STRING;
        String textViewTag = null;
        int backgroundResource = 0;
        int backgroundColor = 0;
        int textColor = 0;
        boolean useBigText = false;
        View.OnClickListener onClickListener = null;

        String conceptUuid = rowData.mConceptUuid == null ? "" : rowData.mConceptUuid;
        String value = rowData.mColumnIdsToValues.get(columnId);
        String localizedValue = rowData.mColumnIdsToLocalizedValues.get(columnId);

        // TODO: Proper localization.
        switch (conceptUuid) {
            case Concepts.TEMPERATURE_UUID:
                String temperatureString = rowData.mColumnIdsToValues.get(columnId);
                if (temperatureString != null) {
                    try {
                        double temperature = Double.parseDouble(temperatureString);
                        text = String.format(Locale.US, "%.1f", temperature);
                        textColor = Color.WHITE;
                        backgroundResource = temperature <= 37.5
                                ? R.drawable.chart_cell_good : R.drawable.chart_cell_bad;
                    } catch (NumberFormatException e) {
                        LOG.w(e, "Temperature format was invalid");
                    }
                }
                break;
            case Concepts.GENERAL_CONDITION_UUID:
                String conditionUuid = rowData.mColumnIdsToValues.get(columnId);
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
                String weightString = rowData.mColumnIdsToValues.get(columnId);
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
                if (rowData.mColumnIdsToValues.containsKey(columnId)  // "any bleeding" option
                        || mColumnIdsWithAnyBleeding.contains(columnId) // specific bleeding options
                ) {
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
            case Concepts.NOTES_UUID:
                if (value != null) {
                    backgroundResource = R.drawable.chart_cell_active_pressable;
                    textViewTag = rowData.mColumnIdsToValues.get(columnId);
                    onClickListener = sNotesOnClickListener;
                }
                break;
            default:
                if (value != null) {
                    backgroundResource = R.drawable.chart_cell_active;
                }
                break;
        }

        if (textView == null) {
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
            textView.setTextSize(useBigText ? TEXT_SIZE_LARGE : TEXT_SIZE_NORMAL);
            textView.setOnClickListener(onClickListener);
        }
        return textView;
    }
}
