package org.projectbuendia.client.ui.chart;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.common.collect.Lists;
import com.mitchellbosecke.pebble.PebbleEngine;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.chrono.ISOChronology;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Chart;
import org.projectbuendia.client.models.ChartItem;
import org.projectbuendia.client.models.ChartSection;
import org.projectbuendia.client.sync.ObsValue;
import org.projectbuendia.client.sync.Order;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

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

/** Renders a patient's chart to HTML displayed in a WebView. */
public class ChartRenderer {
    static PebbleEngine sEngine;
    private static final Logger LOG = Logger.create();

    WebView mView;  // view into which the HTML table will be rendered
    Resources mResources;  // resources used for localizing the rendering
    private List<ObsValue> mLastRenderedObs;  // last set of observations rendered
    private List<Order> mLastRenderedOrders;  // last set of orders rendered
    private Chronology chronology = ISOChronology.getInstance(DateTimeZone.getDefault());

    public interface GridJsInterface {
        @android.webkit.JavascriptInterface void onNewOrderPressed();

        @android.webkit.JavascriptInterface
        void onOrderCellPressed(String orderUuid, long startMillis);
    }

    public ChartRenderer(WebView view, Resources resources) {
        mView = view;
        mResources = resources;
    }

    /** Renders a patient's history of observations to an HTML table in the WebView. */
    public void render(Chart chart, Map<String, ObsValue> latestObservations,
                       List<ObsValue> observations, List<Order> orders,
                       LocalDate admissionDate, LocalDate firstSymptomsDate,
                       GridJsInterface controllerInterface) {

        if (observations.equals(mLastRenderedObs) && orders.equals(mLastRenderedOrders)) {
            return;  // nothing has changed; no need to render again
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
        String html = new GridHtmlGenerator(chart, latestObservations, observations, orders,
                                            admissionDate, firstSymptomsDate).getHtml();
        // If we only call loadData once, the WebView doesn't render the new HTML.
        // If we call loadData twice, it works.  TODO: Figure out what's going on.
        mView.loadDataWithBaseURL("file:///android_asset/", html,
            "text/html; charset=utf-8", "utf-8", null);
        mView.loadDataWithBaseURL("file:///android_asset/", html,
            "text/html; charset=utf-8", "utf-8", null);
        mView.setWebContentsDebuggingEnabled(true);

        mLastRenderedObs = observations;
        mLastRenderedOrders = orders;
    }

    class GridHtmlGenerator {
        List<String> mTileConceptUuids;
        List<String> mGridConceptUuids;
        List<Order> mOrders;
        LocalDate mToday;
        LocalDate mAdmissionDate;
        LocalDate mFirstSymptomsDate;

        List<List<Tile>> mTileRows = new ArrayList<>();
        List<Row> mRows = new ArrayList<>();
        Map<String, Row> mRowsByUuid = new HashMap<>();  // unordered, keyed by concept UUID
        SortedMap<Long, Column> mColumnsByStartMillis = new TreeMap<>();  // ordered by start millis
        SortedSet<LocalDate> mDays = new TreeSet<>();

        GridHtmlGenerator(Chart chart, Map<String, ObsValue> latestObservations,
                          List<ObsValue> observations, List<Order> orders,
                          LocalDate admissionDate, LocalDate firstSymptomsDate) {
            mAdmissionDate = admissionDate;
            mFirstSymptomsDate = firstSymptomsDate;
            mToday = LocalDate.now(chronology);
            mOrders = orders;

            for (ChartSection tileGroup : chart.tileGroups) {
                List<Tile> tileRow = new ArrayList<>();
                for (ChartItem item : tileGroup.items) {
                    ObsValue[] obsValues = new ObsValue[item.conceptUuids.length];
                    for (int i = 0; i < obsValues.length; i++) {
                        obsValues[i] = latestObservations.get(item.conceptUuids[i]);
                    }
                    tileRow.add(new Tile(item, obsValues));
                }
                mTileRows.add(tileRow);
            }
            for (ChartSection chartSection : chart.rowGroups) {
                for (ChartItem chartItem : chartSection.items) {
                    Row row = new Row(chartItem);
                    mRows.add(row);
                    mRowsByUuid.put(chartItem.conceptUuids[0], row);
                }
            }
            addObservations(observations);
        }

        void addObservations(List<ObsValue> observations) {
            for (ObsValue obs : observations) {
                if (obs.value == null) continue;

                mDays.add(obs.obsTime.toLocalDate());
                Column column = getColumnContainingTime(obs.obsTime);
                if (obs.value != null && !obs.value.isEmpty()) {
                    addObs(column, obs);
                }

                if (obs.conceptUuid.equals(AppModel.ORDER_EXECUTED_CONCEPT_UUID)) {
                    Integer count = column.orderExecutionCounts.get(obs.value);
                    column.orderExecutionCounts.put(
                        obs.value, count == null ? 1 : count + 1);
                }
            }
        }

        Column getColumnContainingTime(DateTime dt) {
            LocalDate date = dt.toLocalDate();
            DateTime start = date.toDateTimeAtStartOfDay();
            long startMillis = start.getMillis();
            if (!mColumnsByStartMillis.containsKey(startMillis)) {
                int admitDay = Utils.dayNumberSince(mAdmissionDate, date);
                String admitDayLabel = (admitDay >= 1) ?
                    mResources.getString(R.string.day_n, admitDay) : "–";
                String dateLabel = date.toString("d MMM");
                mColumnsByStartMillis.put(startMillis, new Column(
                    start, start.plusDays(1), admitDayLabel + "<br>" + dateLabel));
            }
            return mColumnsByStartMillis.get(startMillis);
        }

        void addObs(Column column, ObsValue obs) {
            if (!column.obsMap.containsKey(obs.conceptUuid)) {
                column.obsMap.put(obs.conceptUuid, new TreeSet<>(ObsValue.BY_OBS_TIME));
            }
            column.obsMap.get(obs.conceptUuid).add(obs);
        }

        // TODO: grouped coded concepts (for select-multiple, e.g. types of bleeding, types of pain)
        // TODO: concept tags for formatting hints (e.g. none/mild/moderate/severe, abbreviated)
        String getHtml() {
            // Create the list of all the columns to show.  The admission date, the
            // current day, and start and stop days for all orders should be present,
            // as well as any days in between.  TODO: Omit long empty gaps?
            mDays.add(mToday);
            if (mAdmissionDate != null) {
                mDays.add(mAdmissionDate);
            }
            for (Order order : mOrders) {
                if (order.start != null) {
                    mDays.add(order.start.toLocalDate());
                    if (order.stop != null) {
                        mDays.add(order.stop.toLocalDate().plusDays(1));
                    }
                }
            }
            LocalDate lastDay = mDays.last();
            for (LocalDate d = mDays.first(); !d.isAfter(lastDay); d = d.plusDays(1)) {
                getColumnContainingTime(d.toDateTimeAtStartOfDay());
            }

            Map<String, Object> context = new HashMap<>();
            context.put("tileRows", mTileRows);
            context.put("rows", mRows);
            context.put("columns", Lists.newArrayList(mColumnsByStartMillis.values()));
            context.put("nowColumnStart", getColumnContainingTime(DateTime.now()).start);
            context.put("orders", mOrders);
            return renderTemplate("assets/chart.html", context);
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
