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
import org.msf.records.net.model.Concept;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.sync.LocalizedChartHelper.LocalizedObservation;
import org.msf.records.utils.Logger;
import org.msf.records.widget.CellType;
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

        private Row(String conceptUuid, String name) {
            mConceptUuid = conceptUuid;
            mName = name;
        }
    }

    private final HashMap<String, Integer> mDatesToBleedingSiteCount = new HashMap<>();

    private final LayoutInflater mLayoutInflater;
    private final LocalDate today;
    private final List<Row> rows = new ArrayList<>();
    private final List<String> columnHeaders = new ArrayList<>();
    private final Context mContext;

    public LocalizedChartDataGridAdapter(Context context,
                                         List<LocalizedObservation> observations,
                                         LayoutInflater layoutInflater) {
        mContext = context;
        LocalizedChartHelper localizedChartHelper = new LocalizedChartHelper(context.getContentResolver());
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
        for (LocalizedObservation ob : observations) {
            // Observations come through ordered by the chart row, then the observation time, so we
            // want to maintain that order.
            if (row == null || !ob.conceptName.equals(row.mName)) {
                row = new Row(ob.conceptUuid, ob.conceptName);
                rows.add(row);
            }

            if (ob.value == null || LocalizedChartHelper.UNKNOWN_VALUE.equals(ob.value)) {
                // Don't display any dots or handle dates if there are no positive observations.
                continue;
            }

            DateTime d = new DateTime(ob.encounterTimeMillis, chronology);
            LocalDate localDate = d.toLocalDate();
            days.add(localDate);
            String amKey = toAmKey(todayString, localDate, days.first());
            String pmKey = toPmKey(amKey); // this is never displayed to the user
            String dateKey = d.getHourOfDay() < 12 ? amKey : pmKey;

            // Only display dots for positive symptoms.
            if (!LocalizedChartHelper.NO_SYMPTOM_VALUES.contains(ob.value)) {
                // If this is a bleeding site, updating bleeding site counts for this key.
                if (Concept.BLEEDING_SITES_NAME.equals(ob.groupName)
                        && !row.datesToValues.containsKey(dateKey)) {
                    int newCount = mDatesToBleedingSiteCount.containsKey(dateKey)
                            ? mDatesToBleedingSiteCount.get(dateKey) + 1
                            : 1;
                    mDatesToBleedingSiteCount.put(dateKey, newCount);
                }

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
            }
        }
        if (days.isEmpty()) {
            // Just put today, with nothing there.
            String am = toAmKey(todayString, today, today);
            columnHeaders.add(am);
            columnHeaders.add(toPmKey(am));
        } else {
            // Fill in all the columns between start and end.
            for (LocalDate d = days.first(); !d.isAfter(days.last()); d = d.plus(Days.ONE)) {
                String amKey = toAmKey(todayString, d, days.first());
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

    private String toAmKey(String todayString, LocalDate localDate, LocalDate earliestDate) {
        // TODO: Localize.
        String amKey;
        int day = Days.daysBetween(earliestDate, localDate).getDays() + 1;
        int daysDiff = Days.daysBetween(localDate, today).getDays();
        if (daysDiff == 0) {
            amKey = todayString + " (Day " + day + ")\n" + localDate.toString("dd MMM");
        } else {
            amKey = "Day " + day + "\n" + localDate.toString("dd MMM");
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
        // TODO: Proper localization.
        switch (conceptUuid) {
            case Concept.RESPONSIVENESS_UUID:
                String responsivenessString = rowData.datesToValues.get(dateKey);
                if (responsivenessString != null) {
                    text = getLocalizedAvpuInitials(responsivenessString);
                    textColor = Color.BLACK;
                    useBigText = true;
                }
                break;
            case Concept.MOBILITY_UUID:
                String mobilityString = rowData.datesToValues.get(dateKey);
                if (mobilityString != null) {
                    text = getLocalizedMobilityInitials(mobilityString);
                    textColor = Color.BLACK;
                    useBigText = true;
                }
                break;
            case Concept.TEMPERATURE_UUID:
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
            case org.msf.records.model.Concept.GENERAL_CONDITION_UUID:
                String conditionUuid = rowData.datesToValues.get(dateKey);
                if (conditionUuid != null) {
                    ResStatus resStatus = org.msf.records.model.Concept.getResStatus(conditionUuid);
                    ResStatus.Resolved status = resStatus.resolve(mContext.getResources());
                    text = status.getShortDescription().toString();
                    textColor = status.getForegroundColor();
                    backgroundColor = status.getBackgroundColor();
                    useBigText = true;
                }
                break;
            // Concepts with a discrete count value.
            case Concept.DIARRHEA_UUID:
            case Concept.VOMITING_UUID:
                String valueString = rowData.datesToValues.get(dateKey);
                if (valueString != null) {
                    try {
                        int value = Integer.parseInt(valueString);
                        text = String.format(Locale.US, "%d", value);
                        textColor = Color.BLACK;
                        useBigText = true;
                    } catch (NumberFormatException e) {
                        LOG.w(e, "Format for concept %s was invalid", conceptUuid);
                    }
                }
                break;
            case Concept.WEIGHT_UUID:
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
            case Concept.BLEEDING_UUID:
                // Use the exact number of bleeding sites, if available. Otherwise, show one
                // site if "Bleeding (Any)" is specified or 0 otherwise.
                int bleedingSiteCount = mDatesToBleedingSiteCount.containsKey(dateKey)
                        ? mDatesToBleedingSiteCount.get(dateKey)
                        : (Concept.YES_UUID.equals(rowData.datesToValues.get(dateKey)) ? 1 : 0);
                if (bleedingSiteCount == 1) {
                    textColor = Color.BLACK;
                    backgroundColor = mContext.getResources().getColor(R.color.severity_mild);
                } else if (bleedingSiteCount == 2) {
                    textColor = Color.WHITE;
                    backgroundColor = mContext.getResources().getColor(R.color.severity_moderate);
                } else if (bleedingSiteCount >= 3) {
                    textColor = Color.WHITE;
                    backgroundColor = mContext.getResources().getColor(R.color.severity_severe);
                }

                if (bleedingSiteCount != 0) {
                    text = String.format(Locale.US, "%d", Math.min(3, bleedingSiteCount));
                    useBigText = true;
                }
                break;
            case Concept.PAIN_UUID:
            case Concept.WEAKNESS_UUID:
                String valueUuid = rowData.datesToValues.get(dateKey);
                int value = 0;
                if (Concept.MILD_UUID.equals(valueUuid)) {
                    value = 1;
                    textColor = Color.BLACK;
                    // backgroundColor = mContext.getResources().getColor(R.color.severity_mild);
                } else if (Concept.MODERATE_UUID.equals(valueUuid)) {
                    value = 2;
                    textColor = Color.BLACK;
                    // backgroundColor =
                    // mContext.getResources().getColor(R.color.severity_moderate);
                } else if (Concept.SEVERE_UUID.equals(valueUuid)) {
                    value = 3;
                    textColor = Color.RED;
                    // backgroundColor = mContext.getResources().getColor(R.color.severity_severe);
                }

                if (value != 0) {
                    text = String.format(Locale.US, "%d", value);
                    useBigText = true;
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

    private String getLocalizedMobilityInitials(String mobilityUuid) {
        int resId = R.string.mobility_unknown;
        switch (mobilityUuid) {
            case Concept.MOBILITY_WALKING_UUID:
                resId = R.string.mobility_walking;
                break;
            case Concept.MOBILITY_WALKING_WITH_DIFFICULTY_UUID:
                resId = R.string.mobility_walking_with_difficulty;
                break;
            case Concept.MOBILITY_ASSISTED_UUID:
                resId = R.string.mobility_assisted;
                break;
            case Concept.MOBILITY_BED_BOUND_UUID:
                resId = R.string.mobility_bed_bound;
                break;
            default:
                LOG.e("Unrecognized mobility state UUID: %s", mobilityUuid);
        }
        return mContext.getResources().getString(resId);
    }

    private String getLocalizedAvpuInitials(String responsivenessUuid) {
        int resId = R.string.avpu_unknown;
        switch (responsivenessUuid) {
            case Concept.RESPONSIVENESS_ALERT_UUID:
                resId = R.string.avpu_alert;
                break;
            case Concept.RESPONSIVENESS_VOICE_UUID:
                resId = R.string.avpu_voice;
                break;
            case Concept.RESPONSIVENESS_PAIN_UUID:
                resId = R.string.avpu_pain;
                break;
            case Concept.RESPONSIVENESS_UNRESPONSIVE_UUID:
                resId = R.string.avpu_unresponsive;
                break;
            default:
                LOG.e("Unrecognized consciousness state UUID: %s", responsivenessUuid);
        }

        return mContext.getResources().getString(resId);
    }
}
