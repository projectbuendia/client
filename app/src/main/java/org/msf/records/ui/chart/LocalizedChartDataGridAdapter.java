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
import org.msf.records.utils.Utils;
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

        private Row(String conceptUuid, String name) {
            mConceptUuid = conceptUuid;
            mName = name;
        }
    }

    private final HashMap<String, Integer> mColumnIdsToBleedingSiteCounts = new HashMap<>();

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
        final String todayString = context.getResources().getString(R.string.today);
        mToday = LocalDate.now(mChronology);
        for (LocalizedObservation ob : observations) {
            // Observations come through ordered by the chart row, then the observation time, so we
            // want to maintain that order.
            if (row == null || !ob.conceptName.equals(row.mName)) {
                row = new Row(ob.conceptUuid, ob.conceptName);
                mRows.add(row);
            }

            if (ob.value == null || Concepts.UNKNOWN_UUID.equals(ob.value)) {
                // Don't display any dots or handle dates if there are no positive observations.
                continue;
            }

            DateTime obsDateTime = new DateTime(ob.encounterTimeMillis, mChronology);
            String columnId = toColumnId(obsDateTime);
            days.add(obsDateTime.toLocalDate());

            // Only display dots for positive symptoms.
            if (!LocalizedChartHelper.NO_SYMPTOM_VALUES.contains(ob.value)) {
                // If this is a bleeding site, updating bleeding site counts for this key.
                if (Concepts.BLEEDING_SITES_NAME.equals(ob.groupName)
                        && !row.mColumnIdsToValues.containsKey(columnId)) {
                    Integer oldCount = mColumnIdsToBleedingSiteCounts.get(columnId);
                    mColumnIdsToBleedingSiteCounts.put(columnId, (oldCount == null ? 0 : oldCount) + 1);
                }

                // For notes, perform a concatenation rather than overwriting.
                if (Concepts.NOTES_UUID.equals(ob.conceptUuid)) {
                    String oldValue = row.mColumnIdsToValues.get(columnId);
                    row.mColumnIdsToValues.put(
                            columnId, (oldValue == null ? "" : oldValue + "\n–\n") + ob.value);
                } else {
                    row.mColumnIdsToValues.put(columnId, ob.value);
                }
            }
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
        final String todayString = mContext.getResources().getString(R.string.today);
        DateTime dateTime = DateTime.parse(columnId).withChronology(mChronology);
        LocalDate date = dateTime.toLocalDate();

        int admissionDay = Utils.dayNumberSince(mAdmissionDate, date);
        int symptomsDay = Utils.dayNumberSince(mFirstSymptomsDate, date);
        return (admissionDay >= 1 ? "Day " + admissionDay : "–") + "\n"
                + (symptomsDay >= 1 ? symptomsDay : "–") + "\n"
                + (date.equals(mToday) ? todayString + ", " : "")
                + date.toString("MMM d");
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
    public @Nullable TextView fillCell(
            int rowIndex, int columnIndex, View view,
            @Nullable ViewStub viewStub, @Nullable TextView textView) {
        if (viewStub == null && textView == null) {
            throw new IllegalArgumentException("Either viewStub or textView have to be set.");
        }
        final Row rowData = mRows.get(rowIndex);
        final String dateKey = mColumnIds.get(columnIndex);
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
            case Concepts.RESPONSIVENESS_UUID:
                String responsivenessString = rowData.mColumnIdsToValues.get(dateKey);
                if (responsivenessString != null) {
                    text = getLocalizedAvpuInitials(responsivenessString);
                    textColor = Color.BLACK;
                    useBigText = true;
                }
                break;
            case Concepts.MOBILITY_UUID:
                String mobilityString = rowData.mColumnIdsToValues.get(dateKey);
                if (mobilityString != null) {
                    text = getLocalizedMobilityInitials(mobilityString);
                    textColor = Color.BLACK;
                    useBigText = true;
                }
                break;
            case Concepts.TEMPERATURE_UUID:
                String temperatureString = rowData.mColumnIdsToValues.get(dateKey);
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
                String conditionUuid = rowData.mColumnIdsToValues.get(dateKey);
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
                String valueString = rowData.mColumnIdsToValues.get(dateKey);
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
            case Concepts.WEIGHT_UUID:
                String weightString = rowData.mColumnIdsToValues.get(dateKey);
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
                // Use the exact number of bleeding sites, if available. Otherwise, show one
                // site if "Bleeding (Any)" is specified or 0 otherwise.
                int bleedingSiteCount = mColumnIdsToBleedingSiteCounts.containsKey(dateKey)
                        ? mColumnIdsToBleedingSiteCounts.get(dateKey)
                        : (Concepts.YES_UUID.equals(rowData.mColumnIdsToValues.get(dateKey)) ? 1 : 0);
                textColor = Color.BLACK;
                if (bleedingSiteCount >= 3) {
                    textColor = Color.WHITE;
                    backgroundColor = mContext.getResources().getColor(R.color.severity_severe);
                }

                if (bleedingSiteCount != 0) {
                    text = String.format(Locale.US, "%d", bleedingSiteCount);
                    useBigText = true;
                }
                break;
            case Concepts.PAIN_UUID:
            case Concepts.WEAKNESS_UUID:
                String valueUuid = rowData.mColumnIdsToValues.get(dateKey);
                int value = 0;
                if (Concepts.MILD_UUID.equals(valueUuid)) {
                    value = 1;
                    textColor = Color.BLACK;
                    // backgroundColor = mContext.getResources().getColor(R.color.severity_mild);
                } else if (Concepts.MODERATE_UUID.equals(valueUuid)) {
                    value = 2;
                    textColor = Color.BLACK;
                    // backgroundColor =
                    // mContext.getResources().getColor(R.color.severity_moderate);
                } else if (Concepts.SEVERE_UUID.equals(valueUuid)) {
                    value = 3;
                    textColor = Color.RED;
                    // backgroundColor = mContext.getResources().getColor(R.color.severity_severe);
                }

                if (value != 0) {
                    text = String.format(Locale.US, "%d", value);
                    useBigText = true;
                }
                break;
            case Concepts.NOTES_UUID: {
                boolean isActive = rowData.mColumnIdsToValues.containsKey(dateKey);
                if (isActive) {
                    backgroundResource = R.drawable.chart_cell_active_pressable;
                    textViewTag = rowData.mColumnIdsToValues.get(dateKey);
                    onClickListener = notesOnClickListener;
                }
                break;
            }
            default: {
                boolean isActive = rowData.mColumnIdsToValues.containsKey(dateKey);
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
            case Concepts.MOBILITY_WALKING_UUID:
                resId = R.string.mobility_walking;
                break;
            case Concepts.MOBILITY_WALKING_WITH_DIFFICULTY_UUID:
                resId = R.string.mobility_walking_with_difficulty;
                break;
            case Concepts.MOBILITY_ASSISTED_UUID:
                resId = R.string.mobility_assisted;
                break;
            case Concepts.MOBILITY_BED_BOUND_UUID:
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
            case Concepts.RESPONSIVENESS_ALERT_UUID:
                resId = R.string.avpu_alert;
                break;
            case Concepts.RESPONSIVENESS_VOICE_UUID:
                resId = R.string.avpu_voice;
                break;
            case Concepts.RESPONSIVENESS_PAIN_UUID:
                resId = R.string.avpu_pain;
                break;
            case Concepts.RESPONSIVENESS_UNRESPONSIVE_UUID:
                resId = R.string.avpu_unresponsive;
                break;
            default:
                LOG.e("Unrecognized consciousness state UUID: %s", responsivenessUuid);
        }

        return mContext.getResources().getString(resId);
    }
}
