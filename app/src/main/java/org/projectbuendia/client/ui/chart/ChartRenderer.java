package org.projectbuendia.client.ui.chart;

import android.content.res.Resources;
import android.support.v4.util.Pair;
import android.util.DisplayMetrics;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.ReadableInstant;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.AppSettings;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.projectbuendia.client.utils.Utils.HOUR;

/** Renders a patient's chart to HTML displayed in a WebView. */
public class ChartRenderer {
    private static PebbleEngine sEngine;
    private static final Logger LOG = Logger.create();
    public static ZoomLevel[] ZOOM_LEVELS = new ZoomLevel[] {
        new ZoomLevel(R.string.zoom_day, 0),
        new ZoomLevel(R.string.zoom_half, 0, 12*HOUR),
        new ZoomLevel(R.string.zoom_third, 0, 8*HOUR, 16*HOUR),
        new ZoomLevel(R.string.zoom_quarter, 0, 6*HOUR, 12*HOUR, 18*HOUR),
        new ZoomLevel(R.string.zoom_sixth, 0, 4*HOUR, 8*HOUR, 12*HOUR, 16*HOUR, 20*HOUR)
    };

    WebView mView;  // view into which the HTML table will be rendered
    Resources mResources;  // resources used for localizing the rendering
    AppSettings mSettings;

    private List<Obs> mLastRenderedObs;  // last set of observations rendered
    private List<Order> mLastRenderedOrders;  // last set of orders rendered
    private int mLastRenderedZoomIndex;  // last zoom level index rendered
    private String mLastChartName = "";

    public interface GridJsInterface {
        @android.webkit.JavascriptInterface
        void onNewOrderPressed();

        @android.webkit.JavascriptInterface void onOrderHeadingPressed(String orderUuid);

        @android.webkit.JavascriptInterface
        void onOrderCellPressed(String orderUuid, long startMillis);

        @android.webkit.JavascriptInterface
        void onObsDialog(String conceptUuid, String startMillis, String stopMillis);

        @android.webkit.JavascriptInterface
        void onPageUnload(int scrollX, int scrollY);
    }

    public ChartRenderer(WebView view, Resources resources, AppSettings settings) {
        mView = view;
        mResources = resources;
        mSettings = settings;
    }

    /** Renders a patient's history of observations to an HTML table in the WebView. */
    // TODO/cleanup: Have this take the types that getObservations and getLatestObservations return.
    public void render(Chart chart, Map<String, Obs> latestObservations,
                       List<Obs> observations, List<Order> orders,
                       LocalDate admissionDate, LocalDate firstSymptomsDate,
                       GridJsInterface controllerInterface) {
        if (chart == null) {
            mView.loadUrl("file:///android_asset/no_chart.html");
            return;
        }
        LOG.i("Rendering %d observations, %d orders, zoom index %d", observations.size(), orders.size(), mSettings.getChartZoomIndex());
        if (mLastChartName.equals(chart.name) &&
            mLastRenderedZoomIndex == mSettings.getChartZoomIndex() &&
            observations.equals(mLastRenderedObs) &&
            orders.equals(mLastRenderedOrders)) {
            LOG.i("Data and zoom index have not changed; skipping render");
            return;
        }

        // setDefaultFontSize is supposed to take a size in sp, but in practice
        // the fonts don't change size when the user font size preference changes.
        // So, we apply the scaling factor explicitly, defining 1 em to be 10 sp.
        DisplayMetrics metrics = mResources.getDisplayMetrics();
        float defaultFontSize = 10*metrics.scaledDensity/metrics.density;
        mView.getSettings().setDefaultFontSize((int) defaultFontSize);

        mView.getSettings().setJavaScriptEnabled(true);
        mView.addJavascriptInterface(controllerInterface, "controller");
        mView.setWebChromeClient(new WebChromeClient());
        String html = new GridHtmlGenerator(
            chart, latestObservations, observations, orders,
            admissionDate, firstSymptomsDate).getHtml();
        mView.loadDataWithBaseURL(
            "file:///android_asset/", html, "text/html; charset=utf-8", "utf-8", null);
        mView.setWebContentsDebuggingEnabled(true);

        mLastChartName = chart.name;
        mLastRenderedZoomIndex = mSettings.getChartZoomIndex();
        mLastRenderedObs = observations;
        mLastRenderedOrders = orders;
    }

    /** Gets the starting times (in ms) of the segments into which the day is divided. */
    public int[] getSegmentStartTimes() {
        int index = mSettings.getChartZoomIndex();
        return Utils.safeIndex(ZOOM_LEVELS, index).segmentStartTimes;
    }

    class GridHtmlGenerator {
        List<String> mTileConceptUuids;
        List<String> mGridConceptUuids;
        List<Order> mOrders;
        DateTime mNow;
        Column mNowColumn;
        LocalDate mAdmissionDate;
        LocalDate mFirstSymptomsDate;

        Map<String, Multiset<Integer>> mExecutionCounts = new HashMap<>();
        List<List<Tile>> mTileRows = new ArrayList<>();
        List<Row> mRows = new ArrayList<>();
        Map<String, Row> mRowsByUuid = new HashMap<>();  // unordered, keyed by concept UUID
        SortedMap<Long, Column> mColumnsByStartMillis = new TreeMap<>();  // ordered by start millis
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
                    ObsPoint[] points = new ObsPoint[item.conceptUuids.length];
                    for (int i = 0; i < points.length; i++) {
                        Obs obs = latestObservations.get(item.conceptUuids[i]);
                        if (obs != null) {
                            points[i] = obs.getObsPoint();
                        }
                    }
                    tileRow.add(new Tile(item, points));
                    if (!item.script.trim().isEmpty()) {
                        mConceptsToDump.addAll(Arrays.asList(item.conceptUuids));
                    }
                }
                mTileRows.add(tileRow);
            }
            for (ChartSection section : chart.rowGroups) {
                for (ChartItem item : section.items) {
                    Row row = new Row(item);
                    mRows.add(row);
                    mRowsByUuid.put(item.conceptUuids[0], row);
                    if (!item.script.trim().isEmpty()) {
                        mConceptsToDump.addAll(Arrays.asList(item.conceptUuids));
                    }
                }
            }

            Map<String, Order> ordersByUuid = new HashMap<>();
            for (Order order : orders) {
                ordersByUuid.put(order.uuid, order);
            }
            addObservations(observations, ordersByUuid);
            addOrders(orders);
            insertEmptyColumns();
        }

        /** Collects observations into Column objects that make up the grid. */
        void addObservations(List<Obs> observations, Map<String, Order> orders) {
            for (Obs obs : observations) {
                if (obs == null) continue;

                if (obs.conceptUuid.equals(AppModel.ORDER_EXECUTED_CONCEPT_UUID)) {
                    Order order = orders.get(obs.value);
                    if (order != null) {
                        Multiset<Integer> counts = mExecutionCounts.get(order.uuid);
                        if (counts == null) {
                            counts = HashMultiset.<Integer>create();
                            mExecutionCounts.put(order.uuid, counts);
                        }
                        counts.add(order.getDivisionIndex(obs.time));
                    }
                } else {
                    addObs(getColumnContainingTime(obs.time), obs);
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

        /** Gets the n + 1 boundaries of the n segments of a specific day. */
        public DateTime[] getSegmentFenceposts(LocalDate date) {
            DateTime start = date.toDateTimeAtStartOfDay();
            DateTime stop = date.plusDays(1).toDateTimeAtStartOfDay();
            int[] starts = getSegmentStartTimes();
            DateTime[] fenceposts = new DateTime[starts.length + 1];
            for (int i = 0; i < starts.length; i++) {
                fenceposts[i] = start.plusMillis(starts[i]);
            }
            fenceposts[starts.length] = stop;
            return fenceposts;
        }

        /** Finds the segment that contains the given instant. */
        Pair<DateTime, DateTime> getSegmentBounds(ReadableInstant instant) {
            LocalDate date = new DateTime(instant).toLocalDate();  // a day in the local time zone
            DateTime[] fenceposts = getSegmentFenceposts(date);
            for (int i = 0; i + 1 < fenceposts.length; i++) {
                if (!instant.isBefore(fenceposts[i]) && instant.isBefore(fenceposts[i + 1])) {
                    return new Pair<>(fenceposts[i], fenceposts[i + 1]);
                }
            }
            return null;  // should never get here because start <= instant < stop
        }

        /** Gets the column for a given instant, creating columns for the whole day if needed. */
        Column getColumnContainingTime(ReadableInstant instant) {
            LocalDate date = new DateTime(instant).toLocalDate();  // a day in the local time zone
            Pair<DateTime, DateTime> bounds = getSegmentBounds(instant);
            long startMillis = bounds.first.getMillis();
            if (!mColumnsByStartMillis.containsKey(startMillis)) {
                createColumnsForDay(date);
            }
            return mColumnsByStartMillis.get(startMillis);
        }

        /** Creates all the columns for the segments of the given day. */
        void createColumnsForDay(LocalDate date) {
            DateTime[] fenceposts = getSegmentFenceposts(date);
            for (int i = 0; i + 1 < fenceposts.length; i++) {
                DateTime start = fenceposts[i];
                DateTime stop = fenceposts[i + 1];
                Column column = new Column(start, stop, formatDayNumber(date));
                mColumnsByStartMillis.put(column.start.getMillis(), column);
            }
        }

        String formatDayNumber(LocalDate date) {
            int admitDay = Utils.dayNumberSince(mAdmissionDate, date);
            return (admitDay >= 1) ? mResources.getString(R.string.day_n, admitDay) : "";
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
            context.put("now", mNow);
            context.put("tileRows", mTileRows);
            context.put("rows", mRows);
            context.put("columns", Lists.newArrayList(mColumnsByStartMillis.values()));
            context.put("nowColumn", mNowColumn);
            context.put("numColumnsPerDay", getSegmentStartTimes().length);
            context.put("orders", mOrders);
            context.put("executionCounts", mExecutionCounts);
            context.put("dataCellsByConceptId", getJsonDataDump());
            return renderTemplate("assets/chart.html", context);
        }

        /**
         * Inserts empty columns to fill in the gaps between the existing columns, wherever
         * the gap can be filled by inserting fewer than 3 adjacent empty columns.
         */
        void insertEmptyColumns() {
            List<DateTime> starts = new ArrayList<>();
            for (Long startMillis : mColumnsByStartMillis.keySet()) {
                starts.add(new DateTime(startMillis));
            }
            int maxEmptyColumns = 3 / getSegmentStartTimes().length;
            DateTime prev = starts.get(0);
            for (DateTime next : starts) {
                if (!next.isAfter(prev.plusDays(maxEmptyColumns))) {
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
                sEngine = new PebbleEngine.Builder().extension(new PebbleExtension()).build();
            }
            try {
                StringWriter writer = new StringWriter();
                LOG.d("Loading template...");
                PebbleTemplate template = sEngine.getTemplate(filename);
                LOG.d("Evaluating template...");
                template.evaluate(writer, context);
                LOG.d("Template rendered.");
                return writer.toString();
            } catch (Exception e) {
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                return "<div style=\"font-size: 150%\">" + writer.toString().replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
            }
        }
    }

    public static class ZoomLevel {
        public final int labelId;
        public final int[] segmentStartTimes;

        public ZoomLevel(int labelId, int... segmentStartTimes) {
            this.labelId = labelId;
            this.segmentStartTimes = segmentStartTimes;

            if (segmentStartTimes[0] != 0) throw new IllegalArgumentException("First segment must start at time zero");
            int prev = -1;
            for (int next : segmentStartTimes) {
                if (next <= prev) throw new IllegalArgumentException("Segment starting times must strictly increase");
                prev = next;
            }
            if (prev >= 23*HOUR) {
                throw new IllegalArgumentException("Last segment must start before end of a DST-shortened day");
            }
        }
    }
}
