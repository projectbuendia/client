package org.projectbuendia.client.ui.chart;

import android.content.res.Resources;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.common.collect.Lists;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.chrono.ISOChronology;
import org.projectbuendia.client.R;
import org.projectbuendia.client.sync.LocalizedObs;
import org.projectbuendia.client.utils.date.Dates;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** Renders a patient's history of observations to an HTML table displayed in a WebView. */
public class GridRenderer {
    static PebbleEngine sEngine;

    WebView mView;  // view into which the HTML table will be rendered
    Resources mResources;  // resources used for localizing the rendering
    private List<LocalizedObs> mPreviousObservations;  // last set of observations rendered
    private Chronology chronology = ISOChronology.getInstance(DateTimeZone.getDefault());

    public GridRenderer(WebView view, Resources resources) {
        mView = view;
        mResources = resources;
    }

    /** Renders a patient's history of observations to an HTML table in the WebView. */
    public void render(List<LocalizedObs> observations, LocalDate admissionDate, LocalDate firstSymptomsDate) {
        // Avoid redrawing if nothing has changed.
        if (observations.equals(mPreviousObservations)) {
            return;
        }
        mPreviousObservations = observations;
        Map<String, Object> context = gatherTableData(observations, admissionDate, firstSymptomsDate);
        mView.getSettings().setDefaultFontSize(10);  // define 1 em to be equal to 10 sp
        mView.getSettings().setJavaScriptEnabled(true);
        mView.setWebChromeClient(new WebChromeClient());
        mView.loadData(renderTemplate("assets/grid.peb", context), "text/html", null);
    }

    /** Renders a Pebble template. */
    public String renderTemplate(String filename, Map<String, Object> context) {
        if (sEngine == null) {
            // PebbleEngine caches compiled templates by filename, so as long as we keep using the
            // same engine instance, it's okay to call getTemplate(filename) on each render.
            sEngine = new PebbleEngine();
            sEngine.addExtension(new PebbleExtension());
        }
        try {
            StringWriter writer = new StringWriter();
            sEngine.getTemplate(filename).evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            return writer.toString().replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
        }
    }

    // TODO: grouped coded concepts (for select-multiple, e.g. types of bleeding, types of pain)
    // TODO: concept tags for formatting hints (e.g. none/mild/moderate/severe, abbreviated)

    Map<String, Object> gatherTableData(
            List<LocalizedObs> observations, LocalDate admissionDate, LocalDate firstSymptomsDate) {
        LocalDate today = LocalDate.now(chronology);

        List<Row> rows = new ArrayList<>();
        Map<String, Row> rowsByUuid = new HashMap<>();  // unordered, keyed by concept UUID
        SortedMap<String, Column> columnsById = new TreeMap<>();  // ordered by columnId

        TreeSet<LocalDate> days = new TreeSet<>();
        for (LocalizedObs obs : observations) {
            if (obs.value == null) continue;

            Value value = new Value(obs, chronology);
            String columnId = toColumnId(value.observed);
            days.add(value.observed.toLocalDate());

            Row row = rowsByUuid.get(obs.conceptUuid);
            if (row == null) {
                row = new Row(obs.conceptUuid, obs.conceptName);
                rows.add(row);
                rowsByUuid.put(obs.conceptUuid, row);
            }

            Column column = columnsById.get(columnId);
            if (column == null) {
                column = new Column(columnId, formatColumnHeadingHtml(columnId, admissionDate, today));
                columnsById.put(columnId, column);
            }
            SortedSet<Value> values = column.values.get(obs.conceptUuid);
            if (values == null) {
                values = new TreeSet<>(Value.BY_OBS_TIME);
                column.values.put(obs.conceptUuid, values);
            }
            if (value.present) {
                values.add(value);
            }

            /*
            LOG.v("Column: %s, Observation: %s", columnId, ob.toString());
            if (row.mColumnIdsToLocalizedValues.containsKey(columnId)) {
                LOG.v("Overriding previous observation with value: %s",
                        row.mColumnIdsToLocalizedValues.get(columnId));
            }

            // If this is any bleeding site, also show a dot in the "any bleeding" row.
            if (Concepts.BLEEDING_SITES_NAME.equals(ob.groupName)) {
                columnIdsWithAnyBleeding.add(columnId);
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
            */
        }

        // Create the list of all the columns to show.  Today and the admission date should
        // always be present, as well as any days between the last observation and today.
        days.add(today);
        if (admissionDate != null) {
            days.add(admissionDate);
        }
        LocalDate lastDay = days.last();
        //for (LocalDate d = days.first(); !d.isAfter(lastDay); d = d.plus(Days.ONE)) {
        for (LocalDate d : days) {
            DateTime dayStart = d.toDateTimeAtStartOfDay();
            String columnId = toColumnId(dayStart);
            if (!columnsById.containsKey(columnId)) {
                columnsById.put(columnId, new Column(columnId, formatColumnHeadingHtml(columnId, admissionDate, today)));
            }
            columnId = toColumnId(dayStart.withHourOfDay(12));
            if (!columnsById.containsKey(columnId)) {
                columnsById.put(columnId, new Column(columnId, formatColumnHeadingHtml(columnId, admissionDate, today)));
            }
        }

        /*
        // If there are no observations, put some known rows to make it clearer what is being
        // displayed.
        if (rows.isEmpty()) {
            List<LocalizedObs> emptyChart =
                    localizedChartHelper.getEmptyChart(LocalizedChartHelper.ENGLISH_LOCALE);
            for (LocalizedObs ob : emptyChart) {
                rows.add(new Row(ob.conceptUuid, ob.conceptName));
            }
        }
        */
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("columns", Lists.newArrayList(columnsById.values()));  // ordered by columnId
        return data;
    }

    // Determines the column in which a particular DateTime (in the local time zone) belongs,
    // returning a sortable string ID for the column.
    private String toColumnId(DateTime dateTime) {
        int hour = dateTime.getHourOfDay() >= 12 ? 12 : 0;  // choose am or pm column
        return dateTime.withTimeAtStartOfDay().withHourOfDay(hour).toString();
    }

    private String formatColumnHeadingHtml(String columnId, LocalDate admissionDate, LocalDate today) {
        DateTime dateTime = DateTime.parse(columnId).withChronology(chronology);
        LocalDate date = dateTime.toLocalDate();

        int admitDay = Dates.dayNumberSince(admissionDate, date);
        String admitDayLabel = (admitDay >= 1) ? mResources.getString(R.string.day_n, admitDay) : "–";

        String dateString = date.toString("d MMM");
        String dateLabel = date.equals(today)
                ? mResources.getString(R.string.today_date, dateString) : dateString;

        // The column header has two lines of text: the first line gives the day number since
        // admission and the second line gives the calendar date.  Pending feedback from the field
        // on its importance, the symptom onset day number could also be shown in the column
        // header in a different colour.  This would be done by constructing HTML and using
        // Html.fromHtml() to produce a Spanned object that will be rendered by the TextView.
        return admitDayLabel + "<br>" + dateLabel;
    }

    /*
    String renderCell(Row row, String columnId) {
        String conceptUuid = row.mConceptUuid == null ? "" : row.mConceptUuid;
        String value = row.mColumnIdsToValues.get(columnId);
        String localizedValue = row.mColumnIdsToLocalizedValues.get(columnId);

        // TODO: Proper localization.
        switch (conceptUuid) {
            case Concepts.TEMPERATURE_UUID:
                String temperatureString = row.mColumnIdsToValues.get(columnId);
                if (temperatureString != null) {
                    try {
                        double temperature = Double.parseDouble(temperatureString);
                        return String.format(Locale.US, "%.1f", temperature);
                        //textColor = Color.WHITE;
                        //backgroundResource = temperature <= 37.5
                        //        ? R.drawable.chart_cell_good : R.drawable.chart_cell_bad;
                    } catch (NumberFormatException e) {
                        LOG.w(e, "Temperature format was invalid");
                    }
                }
                break;
            case Concepts.GENERAL_CONDITION_UUID:
                String conditionUuid = row.mColumnIdsToValues.get(columnId);
                if (conditionUuid != null) {
                    ResStatus resStatus = Concepts.getResStatus(conditionUuid);
                    ResStatus.Resolved status = resStatus.resolve(mResources);
                    return status.getShortDescription().toString();
                    //textColor = status.getForegroundColor();
                    //backgroundColor = status.getBackgroundColor();
                    //useBigText = true;
                }
                break;
            // Concepts with a discrete count value.
            case Concepts.DIARRHEA_UUID:
            case Concepts.VOMITING_UUID:
                if (value != null) {
                    try {
                        int valueInt = Integer.parseInt(value);
                        return String.format(Locale.US, "%d", valueInt);
                        //textColor = Color.BLACK;
                        //useBigText = true;
                    } catch (NumberFormatException e) {
                        LOG.w(e, "Format for concept %s was invalid", conceptUuid);
                    }
                }
                break;
            case Concepts.WEIGHT_UUID:
                String weightString = row.mColumnIdsToValues.get(columnId);
                if (weightString != null) {
                    try {
                        double weight = Double.parseDouble(weightString);
                        if (weight >= 10) {
                            return String.format(Locale.US, "%d", (int) weight);
                        } else {
                            return String.format(Locale.US, "%.1f", weight);
                        }
                        //textColor = Color.BLACK;
                        // Three digits won't fit in the TextView with the larger font.
                        //if (weight < 100) {
                        //    useBigText = true;
                        //}
                    } catch (NumberFormatException e) {
                        LOG.w(e, "Weight format was invalid");
                    }
                }
                break;
            case Concepts.BLEEDING_UUID:
                if (row.mColumnIdsToValues.containsKey(columnId)  // "any bleeding" option
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
                    return localizedValue.substring(0, abbrevLength);
                    //useBigText = true;
                }
                //textColor = Concepts.SEVERE_UUID.equals(value)
                //        ? Color.RED : Color.BLACK;
                break;
            case Concepts.NOTES_UUID:
                if (value != null) {

                    backgroundResource = R.drawable.chart_cell_active_pressable;
                    textViewTag = row.mColumnIdsToValues.get(columnId);
                    onClickListener = sNotesOnClickListener;
                }
                break;
            default:
                if (value != null) {
                    backgroundResource = R.drawable.chart_cell_active;
                }
                break;
        }
        return "";
    }
    */
}

            /*
            if (mChartView != null) {
                mRootView.removeView(mChartView);
            }
            mChartView = createChartView(observations, admissionDate, firstSymptomsDate);
            mChartView.setLayoutParams(
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            mRootView.addView(mChartView);

        GridRenderer renderer = new GridRenderer()
        mGridWebView.loadData(renderer.render(observations, admissionDate, firstSymptomsDate),
                */

