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
import org.projectbuendia.client.model.Concepts;
import org.projectbuendia.client.sync.LocalizedObs;
import org.projectbuendia.client.sync.Order;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.date.Dates;

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
    private List<LocalizedObs> mRenderedObs;  // last set of observations rendered
    private List<Order> mRenderedOrders;  // last set of orders rendered
    private Chronology chronology = ISOChronology.getInstance(DateTimeZone.getDefault());

    public interface JsInterface {
        @android.webkit.JavascriptInterface
        void onNewOrderPressed();
    }

    public GridRenderer(WebView view, Resources resources) {
        mView = view;
        mResources = resources;
    }

    /**
     * Renders a patient's history of observations to an HTML table in the WebView.
     */
    public void render(List<LocalizedObs> observations, List<Order> orders,
                       LocalDate admissionDate, LocalDate firstSymptomsDate,
                       JsInterface controllerInterface) {
        if (observations.equals(mRenderedObs) && orders.equals(mRenderedOrders)) {
            return;  // nothing has changed; no need to render again
        }
        Map<String, Object> context = gatherTableData(
                observations, orders, admissionDate, firstSymptomsDate);
        mView.getSettings().setDefaultFontSize(10);  // define 1 em to be equal to 10 sp
        mView.getSettings().setJavaScriptEnabled(true);
        mView.addJavascriptInterface(controllerInterface, "controller");
        mView.setWebChromeClient(new WebChromeClient());
        String html = renderTemplate("assets/grid.peb", context);
        // If we only call loadData once, the WebView doesn't render the new HTML.
        // If we call loadData twice, it works.  TODO: Figure out what's going on.
        mView.loadData(html, "text/html", null);
        mView.loadData(html, "text/html", null);

        mRenderedObs = observations;
        mRenderedOrders = orders;
    }

    /**
     * Renders a Pebble template.
     */
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
            List<LocalizedObs> observations, List<Order> orders,
            LocalDate admissionDate, LocalDate firstSymptomsDate) {
        LocalDate today = LocalDate.now(chronology);

        List<Row> rows = new ArrayList<>();
        Map<String, Row> rowsByUuid = new HashMap<>();  // unordered, keyed by concept UUID
        SortedMap<String, Column> columnsById = new TreeMap<>();  // ordered by columnId

        TreeSet<LocalDate> days = new TreeSet<>();
        for (LocalizedObs obs : observations) {
            if (obs.value == null) continue;

            Row row = rowsByUuid.get(obs.conceptUuid);
            if (row == null) {
                row = new Row(obs.conceptUuid, obs.conceptName);
                rows.add(row);
                rowsByUuid.put(obs.conceptUuid, row);
            }

            Value value = new Value(obs, chronology);
            String columnId = toColumnId(value.observed);
            days.add(value.observed.toLocalDate());
            Column column = columnsById.get(columnId);
            if (column == null) {
                column = makeColumn(value.observed, admissionDate);
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

            // If this is any bleeding site, also show a dot in the "any bleeding" row.
            // Note value.bool is a Boolean; if (value.bool) can crash without the null check!
            if (value.bool != null && value.bool && Concepts.BLEEDING_SITES_NAME.equals(obs.groupName)) {
                column.values.put(Concepts.BLEEDING_UUID, ImmutableSortedSet.of(value));
            }
        }

        // Create the list of all the columns to show.  The admission date, the
        // current day, and start and stop days for all orders should be present,
        // as well as any days in between.  TODO: Omit long empty gaps?
        days.add(today);
        if (admissionDate != null) {
            days.add(admissionDate);
        }
        for (Order order : orders) {
            if (order.start != null) {
                days.add(order.start.toLocalDate());
                if (order.stop != null) {
                    days.add(order.stop.toLocalDate().plusDays(1));
                }
            }
        }
        LocalDate lastDay = days.last();
        for (LocalDate d = days.first(); !d.isAfter(lastDay); d = d.plusDays(1)) {
            DateTime dt = d.toDateTimeAtStartOfDay();
            String columnId = toColumnId(dt);
            if (!columnsById.containsKey(columnId)) {
                columnsById.put(columnId, makeColumn(dt, admissionDate));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("columns", Lists.newArrayList(columnsById.values()));  // ordered by columnId
        data.put("nowColumnId", toColumnId(DateTime.now()));
        data.put("orders", orders);
        return data;
    }

    // Determines the column in which a particular DateTime (in the local time zone) belongs,
    // returning a sortable string ID for the column.
    private String toColumnId(DateTime dateTime) {
        return dateTime.toLocalDate().toString();
    }

    private Column makeColumn(DateTime dateTime, LocalDate admissionDate) {
        String columnId = toColumnId(dateTime);
        LocalDate date = dateTime.toLocalDate();

        int admitDay = Dates.dayNumberSince(admissionDate, date);
        String admitDayLabel = (admitDay >= 1) ? mResources.getString(R.string.day_n, admitDay) : "–";
        String dateLabel = date.toString("d MMM");

        // The column header has two lines of text: the first line gives the day number since
        // admission and the second line gives the calendar date.  Pending feedback from the field
        // on its importance, the symptom onset day number could also be shown in the column
        // header in a different colour.
        String headingHtml = admitDayLabel + "<br>" + dateLabel;
        return new Column(columnId, headingHtml,
                date.toDateTimeAtStartOfDay(),
                date.plusDays(1).toDateTimeAtStartOfDay());
    }
}