package org.projectbuendia.client.ui.chart;

import android.content.res.Resources;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.mitchellbosecke.pebble.PebbleEngine;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.chrono.ISOChronology;
import org.projectbuendia.client.R;
import org.projectbuendia.client.data.app.AppModel;
import org.projectbuendia.client.model.Concepts;
import org.projectbuendia.client.sync.LocalizedObs;
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

/** Renders a patient's history of observations to an HTML table displayed in a WebView. */
public class GridRenderer {
    static PebbleEngine sEngine;
    private static final Logger LOG = Logger.create();

    WebView mView;  // view into which the HTML table will be rendered
    Resources mResources;  // resources used for localizing the rendering
    private List<LocalizedObs> mLastRenderedObs;  // last set of observations rendered
    private List<Order> mLastRenderedOrders;  // last set of orders rendered
    private Chronology chronology = ISOChronology.getInstance(DateTimeZone.getDefault());

    public interface GridJsInterface {
        @android.webkit.JavascriptInterface
        void onNewOrderPressed();

        @android.webkit.JavascriptInterface
        void onOrderCellPressed(String orderUuid, long startMillis);
    }

    public GridRenderer(WebView view, Resources resources) {
        mView = view;
        mResources = resources;
    }

    /** Renders a patient's history of observations to an HTML table in the WebView. */
    public void render(List<LocalizedObs> observations, List<Order> orders,
                       LocalDate admissionDate, LocalDate firstSymptomsDate,
                       GridJsInterface controllerInterface) {
        if (observations.equals(mLastRenderedObs) && orders.equals(mLastRenderedOrders)) {
            return;  // nothing has changed; no need to render again
        }
        mView.getSettings().setDefaultFontSize(10);  // define 1 em to be equal to 10 sp
        mView.getSettings().setJavaScriptEnabled(true);
        mView.addJavascriptInterface(controllerInterface, "controller");
        mView.setWebChromeClient(new WebChromeClient());
        String html = new GridHtmlGenerator(
                observations, orders, admissionDate, firstSymptomsDate).getHtml();
        // If we only call loadData once, the WebView doesn't render the new HTML.
        // If we call loadData twice, it works.  TODO: Figure out what's going on.
        mView.loadData(html, "text/html", null);
        mView.loadData(html, "text/html", null);

        mLastRenderedObs = observations;
        mLastRenderedOrders = orders;
    }

    class GridHtmlGenerator {
        List<Order> mOrders;
        LocalDate mToday;
        LocalDate mAdmissionDate;
        LocalDate mFirstSymptomsDate;

        List<Row> mRows = new ArrayList<>();
        Map<String, Row> mRowsByUuid = new HashMap<>();  // unordered, keyed by concept UUID
        SortedMap<Long, Column> mColumnsByStartMillis = new TreeMap<>();  // ordered by start millis
        SortedSet<LocalDate> mDays = new TreeSet<>();

        GridHtmlGenerator(List<LocalizedObs> observations, List<Order> orders,
                          LocalDate admissionDate, LocalDate firstSymptomsDate) {
            mAdmissionDate = admissionDate;
            mFirstSymptomsDate = firstSymptomsDate;
            mToday = LocalDate.now(chronology);
            addObservations(observations);
            mOrders = orders;
        }

        void addObservations(List<LocalizedObs> observations) {
            for (LocalizedObs obs : observations) {
                if (obs.value == null) continue;

                Value value = new Value(obs, chronology);
                mDays.add(value.observed.toLocalDate());
                Column column = getColumnContainingTime(value.observed);
                if (value.present) {
                    addValue(column, obs.conceptUuid, value);
                }

                if (obs.conceptUuid.equals(AppModel.ORDER_EXECUTED_UUID)) {
                    Integer count = column.orderExecutionCounts.get(obs.value);
                    column.orderExecutionCounts.put(
                            obs.value, count == null ? 1 : count + 1);
                } else {
                    addRow(obs.conceptUuid, obs.conceptName);
                }

                // If this is any bleeding site, also show a dot in the "any bleeding" row.
                // Note value.bool is a Boolean; if (value.bool) can crash without the null check!
                if (value.bool != null && value.bool && Concepts.BLEEDING_SITES_NAME.equals(obs.groupName)) {
                    column.values.put(Concepts.BLEEDING_UUID, ImmutableSortedSet.of(value));
                }
            }
        }

        void addValue(Column column, String conceptUuid, Value value) {
            if (!column.values.containsKey(conceptUuid)) {
                column.values.put(conceptUuid, new TreeSet<>(Value.BY_OBS_TIME));
            }
            column.values.get(conceptUuid).add(value);
        }

        void addRow(String conceptUuid, String conceptName) {
            Row row = mRowsByUuid.get(conceptUuid);
            if (row == null) {
                row = new Row(conceptUuid, conceptName);
                mRows.add(row);
                mRowsByUuid.put(conceptUuid, row);
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
            context.put("rows", mRows);
            context.put("columns", Lists.newArrayList(mColumnsByStartMillis.values()));
            context.put("nowColumnStart", getColumnContainingTime(DateTime.now()).start);
            context.put("orders", mOrders);
            return renderTemplate("assets/grid.peb", context);
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
                return writer.toString().replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
            }
        }
    }
}