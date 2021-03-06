package org.projectbuendia.client.ui.chart;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.google.common.collect.Lists;
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
import org.projectbuendia.models.Chart;
import org.projectbuendia.models.ChartItem;
import org.projectbuendia.models.ChartSection;
import org.projectbuendia.models.ConceptUuids;
import org.projectbuendia.models.Obs;
import org.projectbuendia.models.ObsPoint;
import org.projectbuendia.models.ObsValue;
import org.projectbuendia.models.Order;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import static org.projectbuendia.client.utils.Utils.EN_DASH;
import static org.projectbuendia.client.utils.Utils.HOUR;
import static org.projectbuendia.client.utils.Utils.eq;

/** Renders a patient's chart to HTML displayed in a WebView. */
public class ChartRenderer {
    public static ZoomLevel[] ZOOM_LEVELS = new ZoomLevel[] {
        new ZoomLevel(R.string.zoom_day, 0),
        new ZoomLevel(R.string.zoom_half, 0, 12*HOUR),
        new ZoomLevel(R.string.zoom_third, 0, 8*HOUR, 16*HOUR),
        new ZoomLevel(R.string.zoom_bunia_etc, 0, 8*HOUR, 13*HOUR, 18*HOUR),
        new ZoomLevel(R.string.zoom_quarter, 0, 6*HOUR, 12*HOUR, 18*HOUR),
        new ZoomLevel(R.string.zoom_sixth, 0, 4*HOUR, 8*HOUR, 12*HOUR, 16*HOUR, 20*HOUR)
    };

    private static PebbleEngine sEngine;
    private static PebbleTemplate sTemplate;
    private static final Object sTemplateLock = new Object();
    private static final Logger LOG = Logger.create();
    private static final ExecutionHistory EMPTY_HISTORY = new ExecutionHistory();
    private static final String TEMPLATE_PATH = "assets/chart.html";

    private WebView mView;  // view into which the HTML table will be rendered
    private Resources mResources;  // resources used for localizing the rendering
    private AppSettings mSettings;

    private List<Obs> mLastRenderedObs;  // last set of observations rendered
    private List<Order> mLastRenderedOrders;  // last set of orders rendered
    private int mLastRenderedZoomIndex;  // last zoom level index rendered
    private String mLastChartName = "";

    // WARNING(kpy): The WebView only knows how to pass boolean, numeric, or
    // String arguments to these methods from JavaScript, and does not always
    // perform the obvious conversions.  Every parameter must be declared here
    // as boolean, int, long, float, double, or String.  Methods must be called
    // with exactly the declared number of arguments, or the call will cause
    // a JavaScript "Method not found" exception.  Boolean parameters convert
    // all non-boolean values to false.  Numeric parameters convert incoming
    // numbers to the declared type and all non-numeric values to 0.  String
    // parameters pass through nulls, convert all booleans and numbers to
    // strings, and convert anything else to the string "undefined".
    public interface JsInterface {
        @JavascriptInterface void onNewOrderPressed();
        @JavascriptInterface void onOrderHeadingPressed(String orderUuid);
        @JavascriptInterface void onOrderCellPressed(String orderUuid, long startMillis);
        @JavascriptInterface void showObsDialog(String conceptUuids);
        @JavascriptInterface void showObsDialog(long startMillis, long stopMillis);
        @JavascriptInterface void showObsDialog(String conceptUuids, long startMillis, long stopMillis);
        @JavascriptInterface void onPageUnload(int scrollX, int scrollY);
    }

    public ChartRenderer(WebView view, Resources resources, AppSettings settings) {
        mView = view;
        mResources = resources;
        mSettings = settings;
        mView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        mView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    /** Renders a patient's history of observations to an HTML table in the WebView. */
    // TODO/cleanup: Have this take the types that getObservations and getLatestObservations return.
    public void render(Chart chart, Map<String, Obs> latestObservations,
                       @Nonnull List<Obs> observations, @Nonnull List<Order> orders,
                       JsInterface controllerInterface) {
        if (chart == null) {
            mView.loadUrl("file:///android_asset/no_chart.html");
            return;
        }
        if (eq(mLastChartName, chart.name) &&
            mLastRenderedZoomIndex == mSettings.getChartZoomIndex() &&
            eq(observations, mLastRenderedObs) &&
            eq(orders, mLastRenderedOrders)) {
            LOG.i("%d observations, %d orders, and zoom index %d unchanged; skipping render", observations.size(), orders.size(), mSettings.getChartZoomIndex());
            return;
        }

        LOG.start("render", "%d observations, %d orders, zoom index %d", observations.size(), orders.size(), mSettings.getChartZoomIndex());

        // setDefaultFontSize is supposed to take a size in sp, but in practice
        // the fonts don't change size when the user font size preference changes.
        // So, we apply the scaling factor explicitly, defining 1 em to be 10 sp.
        DisplayMetrics metrics = mResources.getDisplayMetrics();
        float defaultFontSize = 10*metrics.scaledDensity/metrics.density;
        mView.getSettings().setDefaultFontSize((int) defaultFontSize);

        mView.getSettings().setJavaScriptEnabled(true);
        mView.addJavascriptInterface(controllerInterface, "c");
        mView.setWebChromeClient(new WebChromeClient());
        String html = new GridHtmlGenerator(
            chart, latestObservations, observations, orders).getHtml();

        LOG.elapsed("render", "HTML generated");

        // Record the scroll position of the viewport in the document so we can restore it.
        mView.evaluateJavascript("getScrollPosition()", (value) -> {
            value = value.replace('"', ' ').trim();
            String scrollJs = "<script>setScrollPosition('" + value + "');</script>";
            LOG.i("scrollJs = %s", Utils.repr(scrollJs));

            // To avoid showing stale, possibly misleading data from a previous
            // patient, clear out any previous chart HTML before showing the WebView.
            mView.loadUrl("about:blank");
            mView.clearView();
            mView.setVisibility(View.VISIBLE);
            mView.loadDataWithBaseURL(
                "file:///android_asset/", html + scrollJs, "text/html; charset=utf-8", "utf-8", null);
            mView.setWebContentsDebuggingEnabled(true);

            LOG.finish("render", "HTML loaded into WebView");

            mLastChartName = chart.name;
            mLastRenderedZoomIndex = mSettings.getChartZoomIndex();
            mLastRenderedObs = observations;
            mLastRenderedOrders = orders;

            LOG.start("ChartJS");
        });
    }

    /** Gets the starting times (in ms) of the segments into which the day is divided. */
    public int[] getSegmentStartTimes() {
        int index = mSettings.getChartZoomIndex();
        return Utils.safeIndex(ZOOM_LEVELS, index).segmentStartTimes;
    }

    class GridHtmlGenerator {
        List<Order> mOrders;
        DateTime mNow;
        Column mNowColumn;
        DateTime mAdmissionDateTime;

        Map<String, ExecutionHistory> mExecutionHistories = new HashMap<String, ExecutionHistory>() {
            @Override public @NonNull ExecutionHistory get(Object key) {
                // Always return non-null, for convenient access from the Pebble template.
                return Utils.orDefault(super.get(key), EMPTY_HISTORY);
            }
        };
        List<List<Tile>> mTileRows = new ArrayList<>();
        List<List<Tile>> mFixedRows = new ArrayList<>();
        List<RowGroup> mRowGroups = new ArrayList<>();
        SortedMap<Long, Column> mColumnsByStartMillis = new TreeMap<>();  // ordered by start millis
        Set<String> mConceptsToDump = new HashSet<>();  // concepts whose data to dump in JSON

        GridHtmlGenerator(Chart chart, Map<String, Obs> latestObservations,
                          List<Obs> observations, List<Order> orders) {
            LOG.start("GridHtmlGenerator");

            mOrders = orders;
            mNow = DateTime.now();
            Obs obs = latestObservations.get(ConceptUuids.ADMISSION_DATETIME_UUID);
            mAdmissionDateTime = obs != null ? Utils.toLocalDateTime(Long.valueOf(obs.value)) : null;
            mNowColumn = getColumnContainingTime(mNow); // ensure there's a column for today

            for (ChartSection section : chart.fixedGroups) {
                mFixedRows.add(createTiles(section, latestObservations));
            }
            for (ChartSection section : chart.tileGroups) {
                mTileRows.add(createTiles(section, latestObservations));
            }
            for (ChartSection section : chart.rowGroups) {
                mRowGroups.add(createRowGroup(section));
            }

            Map<String, Order> ordersByUuid = new HashMap<>();
            for (Order order : orders) {
                ordersByUuid.put(order.uuid, order);
            }
            addObservations(observations, ordersByUuid);
            addOrders(orders);
            insertEmptyColumns();

            LOG.elapsed("GridHtmlGenerator", "Data prepared");
        }

        private List<Tile> createTiles(ChartSection section, Map<String, Obs> observations) {
            List<Tile> tiles = new ArrayList<>();
            for (ChartItem item : section.items) {
                List<ObsPoint> points = new ArrayList<>();
                for (String uuid : item.conceptUuids) {
                    Obs obs = observations.get(uuid);
                    if (eq(uuid, ConceptUuids.PLACEMENT_UUID)) {
                        // Special case: a placement value is split into
                        // two values, a location and a bed number.
                        if (obs == null) {
                            points.add(null);
                            points.add(null);
                            continue;
                        }
                        String value = obs.getObsValue().text;
                        String[] parts = Utils.splitFields(value, "/", 2);
                        points.add(new ObsPoint(obs.time, ObsValue.newText(parts[0])));
                        points.add(new ObsPoint(obs.time, ObsValue.newText(parts[1])));
                    } else {
                        points.add(obs != null ? obs.getObsPoint() : null);
                    }
                }
                tiles.add(new Tile(item, points.toArray(new ObsPoint[0])));
                if (!item.script.trim().isEmpty()) {
                    mConceptsToDump.addAll(Arrays.asList(item.conceptUuids));
                }
            }
            return tiles;
        }

        private RowGroup createRowGroup(ChartSection section) {
            RowGroup rowGroup = new RowGroup(section.label);
            for (ChartItem item : section.items) {
                rowGroup.rows.add(new Row(item));
                if (!item.script.trim().isEmpty()) {
                    mConceptsToDump.addAll(Arrays.asList(item.conceptUuids));
                }
            }
            return rowGroup;
        }

        /** Collects observations into Column objects that make up the grid. */
        void addObservations(List<Obs> observations, Map<String, Order> orders) {
            for (Obs obs : observations) {
                if (obs == null) continue;

                if (obs.conceptUuid.equals(ConceptUuids.ORDER_EXECUTED_UUID)) {
                    Order order = orders.get(obs.orderUuid);
                    if (order != null) {
                        if (!mExecutionHistories.containsKey(order.uuid)) {
                            mExecutionHistories.put(order.uuid, new ExecutionHistory(order));
                        }
                        mExecutionHistories.get(order.uuid).add(obs.time);
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
            LocalDate date = Utils.toLocalDateTime(instant).toLocalDate();  // a day in the local time zone
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
            LocalDate date = Utils.toLocalDateTime(instant).toLocalDate();  // a day in the local time zone
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
            if (mAdmissionDateTime == null) return EN_DASH;
            int admitDay = Utils.dayNumberSince(mAdmissionDateTime.toLocalDate(), date);
            return (admitDay >= 1) ? mResources.getString(R.string.day_n, admitDay) : "";
        }

        void addObs(Column column, Obs obs) {
            if (!column.pointSetByConceptUuid.containsKey(obs.conceptUuid)) {
                column.pointSetByConceptUuid.put(obs.conceptUuid, new TreeSet<>());
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
            context.put("today", mNow.toLocalDate());
            context.put("fixedRows", mFixedRows);
            context.put("tileRows", mTileRows);
            context.put("rowGroups", mRowGroups);
            context.put("columns", Lists.newArrayList(mColumnsByStartMillis.values()));
            context.put("nowColumn", mNowColumn);
            context.put("numColumnsPerDay", getSegmentStartTimes().length);
            context.put("dataCellsByConceptId", getJsonDataDump());
            context.put("orders", getSortedOrders());
            context.put("executionHistories", getSortedExecutionHistories());
            LOG.elapsed("GridHtmlGenerator", "Template context populated");
            String result = renderTemplate(context);
            LOG.finish("GridHtmlGenerator", "Finished rendering HTML");
            return result;
        }

        List<Order> getSortedOrders() {
            List<Order> sortedOrders = new ArrayList<>(mOrders);
            Collections.sort(sortedOrders, (a, b) -> {
                int result = a.instructions.route.compareTo(b.instructions.route);
                if (result != 0) return result;
                return a.start.compareTo(b.start);
            });
            return sortedOrders;
        }

        Map<String, ExecutionHistory> getSortedExecutionHistories() {
            for (ExecutionHistory history : mExecutionHistories.values()) {
                history.sort();
            }
            return mExecutionHistories;
        }

        /**
         * Inserts empty columns to fill in the gaps between the existing columns, wherever
         * the gap can be filled by inserting fewer than 3 adjacent empty columns.
         */
        void insertEmptyColumns() {
            List<DateTime> starts = new ArrayList<>();
            for (Long startMillis : mColumnsByStartMillis.keySet()) {
                starts.add(Utils.toLocalDateTime(startMillis));
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

        /** Renders the Pebble template for the chart. */
        String renderTemplate(Map<String, Object> context) {
            LOG.start("renderTemplate");
            foregroundCompileTemplate();
            try {
                StringWriter writer = new StringWriter();
                LOG.elapsed("renderTemplate", "Template loaded");
                sTemplate.evaluate(writer, context);
                LOG.finish("renderTemplate");
                return writer.toString();
            } catch (Exception e) {
                StringWriter writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                return "<div style=\"font-size: 150%\">" + writer.toString().replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
            }
        }
    }

    public static void backgroundCompileTemplate() {
        if (sEngine == null || sTemplate == null) {
            new AsyncTask<Void, Void, Void>() {
                public Void doInBackground(Void... param) {
                    compileTemplate();
                    return null;
                }
            }.execute();
        }
    }

    public static void foregroundCompileTemplate() {
        if (sEngine == null || sTemplate == null) {
            compileTemplate();
        }
    }

    private static void compileTemplate() {
        synchronized (sTemplateLock) {
            if (sEngine == null || sTemplate == null) {
                LOG.start("compileTemplate");
                sEngine = new PebbleEngine.Builder().extension(new PebbleExtension()).build();
                sTemplate = sEngine.getTemplate(TEMPLATE_PATH);
                LOG.finish("compileTemplate");
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
