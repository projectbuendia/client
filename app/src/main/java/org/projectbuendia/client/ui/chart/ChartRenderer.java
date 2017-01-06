package org.projectbuendia.client.ui.chart;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.common.collect.Lists;
import com.mitchellbosecke.pebble.PebbleEngine;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.ReadableInstant;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Chart;
import org.projectbuendia.client.models.ChartItem;
import org.projectbuendia.client.models.ChartSection;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.ObsPoint;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/** Renders a patient's chart to HTML displayed in a WebView. */
public class ChartRenderer {
    static PebbleEngine sEngine;
    private static final Logger LOG = Logger.create();

    WebView mView;  // view into which the HTML table will be rendered
    Resources mResources;  // resources used for localizing the rendering
    private List<Obs> mLastRenderedObs;  // last set of observations rendered
    private List<Order> mLastRenderedOrders;  // last set of orders rendered
    private String lastChart = "";
    private String mChartColumnType;
    private int mChartColumnTime;

    public interface GridJsInterface {
        @android.webkit.JavascriptInterface
        void onNewOrderPressed();

        @android.webkit.JavascriptInterface void onOrderHeadingPressed(String orderUuid);

        @android.webkit.JavascriptInterface
        void onOrderCellPressed(String orderUuid, long startMillis);

        @android.webkit.JavascriptInterface
        void onObsDialog(String conceptUuid);

        @android.webkit.JavascriptInterface
        void onObsDialog(String conceptUuid, long startMillis, long stopMillis);

        @android.webkit.JavascriptInterface
        void onPageUnload(int scrollX, int scrollY);
    }

    public ChartRenderer(WebView view, Resources resources) {
        mView = view;
        mResources = resources;
    }

    /** Renders a patient's history of observations to an HTML table in the WebView. */
    // TODO/cleanup: Have this take the types that getObservations and getLatestObservations return.
    @SuppressLint("SetJavaScriptEnabled")
    public void render(Chart chart, Map<String, Obs> latestObservations,
                       List<Obs> observations, List<Order> orders,
                       LocalDate admissionDate, LocalDate firstSymptomsDate,
                       GridJsInterface controllerInterface) {
        if (chart == null) {
            mView.loadUrl("file:///android_asset/no_chart.html");
            return;
        }

        if (observations.equals(mLastRenderedObs)
                && orders.equals(mLastRenderedOrders)
                && Objects.equals(lastChart, chart.name)) {
            return;  // nothing has changed; no need to render again
        }
        lastChart = chart.name;
        mChartColumnType = chart.columnType;
        mChartColumnTime = chart.columnTime;

        // setDefaultFontSize is supposed to take a size in sp, but in practice
        // the fonts don't change size when the user font size preference changes.
        // So, we apply the scaling factor explicitly, defining 1 em to be 10 sp.
        DisplayMetrics metrics = mResources.getDisplayMetrics();
        float defaultFontSize = 10*metrics.scaledDensity/metrics.density;
        mView.getSettings().setDefaultFontSize((int) defaultFontSize);

        mView.getSettings().setJavaScriptEnabled(true);
        mView.addJavascriptInterface(controllerInterface, "controller");
        mView.setWebChromeClient(new WebChromeClient());
        String html = new GridHtmlGenerator(chart, latestObservations, observations, orders,
                                            admissionDate, firstSymptomsDate).getHtml();
        mView.loadDataWithBaseURL("file:///android_asset/", html,
            "text/html; charset=utf-8", "utf-8", null);
        WebView.setWebContentsDebuggingEnabled(true);

        mLastRenderedObs = observations;
        mLastRenderedOrders = orders;
    }

    class GridHtmlGenerator {
        List<Order> mOrders;
        DateTime mNow;
        Column mNowColumn;
        LocalDate mAdmissionDate;
        LocalDate mFirstSymptomsDate;

        List<List<Tile>> mTileRows = new ArrayList<>();
        List<org.projectbuendia.client.ui.chart.Row> mRows = new ArrayList<>();
        Map<String, org.projectbuendia.client.ui.chart.Row> mRowsByUuid = new HashMap<>();  // unordered, keyed by concept UUID

        private final Comparator<Interval> mIntervalComparator = new Comparator<Interval>() {
            @Override public int compare(Interval interval1, Interval interval2) {
                return Long.compare(interval1.getStartMillis(), interval2.getStartMillis());
            }
        };
        SortedMap<Interval, Column> mColumnsByStartMillis = new TreeMap<>(mIntervalComparator);

        Set<String> mConceptsToDump = new HashSet<>();  // concepts whose data to dump in JSON

        GridHtmlGenerator(Chart chart, Map<String, Obs> latestObservations,
                          List<Obs> observations, List<Order> orders,
                          LocalDate admissionDate, LocalDate firstSymptomsDate) {
            mAdmissionDate = admissionDate;
            mFirstSymptomsDate = firstSymptomsDate;
            mOrders = orders;
            mNow = DateTime.now();
            mNowColumn = getColumnContainingTime(mNow); // ensure there's a column for today

            for (ChartSection tileGroup : chart.tileGroups) {
                List<Tile> tileRow = new ArrayList<>();
                for (ChartItem item : tileGroup.items) {
                    ObsPoint[] points = new ObsPoint[item.conceptUuids.size()];
                    for (int i = 0; i < points.length; i++) {
                        Obs obs = latestObservations.get(item.conceptUuids.get(i));
                        if (obs != null) {
                            points[i] = obs.getObsPoint();
                        }
                    }
                    tileRow.add(new Tile(item, points));
                    if (!item.script.trim().isEmpty()) {
                        mConceptsToDump.addAll(item.conceptUuids);
                    }
                }
                mTileRows.add(tileRow);
            }
            for (ChartSection section : chart.rowGroups) {
                for (ChartItem item : section.items) {
                    Row row = new Row(item);
                    mRows.add(row);
                    mRowsByUuid.put(item.conceptUuids.get(0), row);
                    if (!item.script.trim().isEmpty()) {
                        mConceptsToDump.addAll(item.conceptUuids);
                    }
                }
            }
            addObservations(observations);
            addOrders(orders);
            insertEmptyColumns();
        }

        void addObservations(List<Obs> observations) {
            for (Obs obs : observations) {
                if (obs == null) continue;
                Column column = getColumnContainingTime(obs.time);

                if (obs.conceptUuid.equals(AppModel.ORDER_EXECUTED_CONCEPT_UUID)) {
                    Integer count = column.executionCountsByOrderUuid.get(obs.value);
                    column.executionCountsByOrderUuid.put(
                        obs.value, count == null ? 1 : count + 1);
                } else {
                    addObs(column, obs);
                }
            }
        }

        /** Ensures that columns are shown for any days in which an order is prescribed. */
        void addOrders(List<Order> orders) {
            for (Order order : orders) {
                if (order.stop != null) {
                    for (DateTime dt = order.start; !dt.isAfter(order.stop.plusDays(1)); dt = dt.plusDays(1)) {
                        getColumnContainingTime(dt); // creates the column if it doesn't exist
                    }
                }
            }
        }

        /** Returns the column that contains the given instant, creating it if it doesn't exist. */
        Column getColumnContainingTime(ReadableInstant instant) {
            Column returnColumn;
            switch (mChartColumnType) {
                case Column.ENCOUNTER:
                    returnColumn =  getEncounterColumn(instant);
                    break;
                case Column.TIMED:
                    returnColumn =  getTimedColumn(instant);
                    break;
                case Column.DAILY:
                default:
                    returnColumn =  getDailyColumn(instant);
                    break;
            }
            return returnColumn;
        }

        Column getDailyColumn(ReadableInstant instant) {
            LocalDateTime date = new DateTime(instant).toLocalDateTime();  // a day in the local time zone
            DateTime start = date.withTime(0, 0, 0, 0).toDateTime();
            DateTime end = date.millisOfDay().withMaximumValue().toDateTime();
            long startMillis = start.getMillis();
            long endMillis = end.getMillis();
            Interval columnInterval = new Interval(startMillis, endMillis);

            if (!mColumnsByStartMillis.containsKey(columnInterval)) {
                int admitDay = Utils.dayNumberSince(mAdmissionDate, date.toLocalDate());
                String admitDayLabel = (admitDay >= 1) ?
                    mResources.getString(R.string.day_n, admitDay) : "–";
                String dateLabel = date.toString("d MMM");
                mColumnsByStartMillis.put(columnInterval, new Column(
                    columnInterval, admitDayLabel + "<br/>" + dateLabel));
            }
            return mColumnsByStartMillis.get(columnInterval);
        }

        Column getEncounterColumn(ReadableInstant instant) {
            DateTime date = new DateTime(instant);
            DateTime start = date.withTime(date.getHourOfDay(), date.getMinuteOfHour(), 0, 0).toDateTime();
            DateTime end = start.plusMinutes(mChartColumnTime);
            Interval columnInterval = new Interval(start, end);
            if (!mColumnsByStartMillis.containsKey(columnInterval)) {
                String dateLabel = start.toString("d MMM");
                String timeLabel = start.toString("HH:mm");
                mColumnsByStartMillis.put(columnInterval, new Column(
                    columnInterval, dateLabel + "<br/>" + timeLabel));
            }
            return mColumnsByStartMillis.get(columnInterval);
        }

        Column getTimedColumn(ReadableInstant instant) {
            DateTime date = new DateTime(instant);
            DateTime hour = date.hourOfDay().roundFloorCopy();
            long millisSinceHour = new Duration(hour, date).getMillis();
            int roundedMinutes = ((int)Math.floor(
                millisSinceHour / 60000.0 / mChartColumnTime)) * mChartColumnTime;
            DateTime start = hour.plusMinutes(roundedMinutes);
            DateTime end = start.plusMinutes(mChartColumnTime);
            Interval columnInterval = new Interval(start, end);
            if (!mColumnsByStartMillis.containsKey(columnInterval)) {
                String dateLabel = start.toString("d MMM");
                String timeLabel = start.toString("HH:mm");
                mColumnsByStartMillis.put(columnInterval, new Column(
                    columnInterval, dateLabel + "<br/>" + timeLabel));
            }
            return mColumnsByStartMillis.get(columnInterval);
        }

        void addObs(Column column, Obs obs) {
            if (!column.pointSetByConceptUuid.containsKey(obs.conceptUuid)) {
                column.pointSetByConceptUuid.put(obs.conceptUuid, new TreeSet<ObsPoint>());
            }
            ObsPoint point = obs.getObsPoint();
            if (point != null) {
                column.pointSetByConceptUuid.get(obs.conceptUuid).add(point);
            }
        }

        /** Exports a map of concept IDs to arrays of [columnStart, points] pairs. */
        JSONObject getJsonDataDump() {
            JSONObject dump = new JSONObject();
            for (String uuid : mConceptsToDump) {
                try {
                    JSONArray pointGroups = new JSONArray();
                    for (Column column : mColumnsByStartMillis.values()) {
                        JSONArray pointArray = new JSONArray();
                        SortedSet<ObsPoint> points = column.pointSetByConceptUuid.get(uuid);
                        if (points != null && points.size() > 0) {
                            for (ObsPoint point : points) {
                                pointArray.put(point.toJson());
                            }
                            JSONObject pointGroup = new JSONObject();
                            pointGroup.put("start", column.start.getMillis());
                            pointGroup.put("stop", column.stop.getMillis());
                            pointGroup.put("points", pointArray);
                            pointGroups.put(pointGroup);
                        }
                    }
                    dump.put("" + Utils.compressUuid(uuid), pointGroups);
                } catch (JSONException e) {
                    LOG.e(e, "JSON error while dumping chart data");
                }
            }
            return dump;
        }

        // TODO: grouped coded concepts (for select-multiple, e.g. types of bleeding, types of pain)
        // TODO: concept tags for formatting hints (e.g. none/mild/moderate/severe, abbreviated)
        String getHtml() {
            Map<String, Object> context = new HashMap<>();
            context.put("tileRows", mTileRows);
            context.put("rows", mRows);
            context.put("columns", Lists.newArrayList(mColumnsByStartMillis.values()));
            context.put("nowColumnStart", mNowColumn.start);
            context.put("orders", mOrders);
            context.put("dataCellsByConceptId", getJsonDataDump());
            return renderTemplate("assets/chart.html", context);
        }

        /**
         * Inserts empty columns to fill in the gaps between the existing columns, wherever
         * the gap can be filled by inserting fewer than 3 adjacent empty columns.
         */
        void insertEmptyColumns() {
            List<DateTime> starts = new ArrayList<>();
            for (Interval i : mColumnsByStartMillis.keySet()) {
                Long startMillis = i.getStartMillis();
                starts.add(new DateTime(startMillis));
            }
            DateTime prev = starts.get(0);
            for (DateTime next : starts) {
                if (!next.isAfter(prev.plusDays(3))) {
                    for (DateTime dt = prev.plusDays(1); dt.isBefore(next); dt = dt.plusDays(1)) {
                        getColumnContainingTime(dt); // creates a column if it doesn't exist yet
                    }
                }
                prev = next;
            }
        }

        /** Renders a Pebble template. */
        String renderTemplate(String filename, Map<String, Object> context) {
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
                return "<div style=\"font-size: 150%\">" + writer.toString().replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
            }
        }
    }
}
