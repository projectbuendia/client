package org.projectbuendia.client.ui.chart;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
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
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

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

/** Renders a patient's chart to HTML displayed in a WebView. */
public class ChartRenderer {
    private static final Logger LOG = Logger.create();

    WebView mView;  // view into which the HTML table will be rendered
    Resources mResources;  // resources used for localizing the rendering
    private List<Obs> mLastRenderedObs;  // last set of observations rendered
    private List<Order> mLastRenderedOrders;  // last set of orders rendered

    public interface GridJsInterface {
        @android.webkit.JavascriptInterface void onNewOrderPressed();

        @android.webkit.JavascriptInterface void onOrderHeadingPressed(String orderUuid);

        @android.webkit.JavascriptInterface void onOrderCellPressed(String orderUuid, long startMillis);
    }

    public ChartRenderer(WebView view, Resources resources) {
        mView = view;
        mResources = resources;
    }

    /** Renders a patient's history of observations to an HTML table in the WebView. */
    // TODO/cleanup: Have this take the types that getObservations and getLatestObservations return.
    public void render(Chart chart, Patient patient, Map<String, Obs> latestObservations,
                       List<Obs> observations, List<Order> orders,
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
        String html = new GridHtmlGenerator(chart, patient, latestObservations,
            observations, orders, admissionDate, firstSymptomsDate).getHtml();
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
        DateTime mNow;
        Column mNowColumn;
        Patient mPatient;
        LocalDate mAdmissionDate;
        LocalDate mFirstSymptomsDate;

        List<List<Tile>> mTileRows = new ArrayList<>();
        List<Row> mRows = new ArrayList<>();
        Map<String, Row> mRowsByUuid = new HashMap<>();  // unordered, keyed by concept UUID
        SortedMap<Long, Column> mColumnsByStartMillis = new TreeMap<>();  // ordered by start millis
        Set<String> mConceptsToDump = new HashSet<>();  // concepts whose data to dump in JSON

        GridHtmlGenerator(Chart chart, Patient patient, Map<String, Obs> latestObservations,
                          List<Obs> observations, List<Order> orders,
                          LocalDate admissionDate, LocalDate firstSymptomsDate) {
            mAdmissionDate = admissionDate;
            mFirstSymptomsDate = firstSymptomsDate;
            mPatient = patient;
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
                    for (DateTime dt = order.start; !dt.isAfter(order.stop); dt = dt.plusDays(1)) {
                        getColumnContainingTime(dt); // creates the column if it doesn't exist
                    }
                }
            }
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
            context.put("patientData", getPatientData());
            context.put("conceptData", getConceptData());
            return Utils.renderTemplate("chart.html", context);
        }

        /** Returns the column that contains the given instant, creating it if it doesn't exist. */
        Column getColumnContainingTime(ReadableInstant instant) {
            LocalDate date = new DateTime(instant).toLocalDate();  // a day in the local time zone
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

        void addObs(Column column, Obs obs) {
            if (!column.pointSetByConceptUuid.containsKey(obs.conceptUuid)) {
                column.pointSetByConceptUuid.put(obs.conceptUuid, new TreeSet<ObsPoint>());
            }
            ObsPoint point = obs.getObsPoint();
            if (point != null) {
                column.pointSetByConceptUuid.get(obs.conceptUuid).add(point);
            }
        }

        JSONObject getPatientData() {
            JSONObject result = new JSONObject();
            try {
                result.put("uuid", mPatient.uuid);
                result.put("given_name", mPatient.givenName);
                result.put("family_name", mPatient.familyName);
                result.put("gender", mPatient.gender == Patient.GENDER_FEMALE ? "F" :
                    mPatient.gender == Patient.GENDER_MALE ? "M" : "U");
                if (mPatient.birthdate != null) {
                    result.put("birthdate", "" + mPatient.birthdate);
                    Period age = new Period(mPatient.birthdate, LocalDate.now());
                    result.put("age_months", age.getYears()*12 + age.getMonths());
                    result.put("age_years", age.getYears() + age.getMonths()/12.0);
                }
                result.put("location_uuid", mPatient.locationUuid);
            } catch (JSONException e) {
                LOG.e(e, "JSON error while dumping patient data");
            }
            return result;
        }

        /** Exports a map of concept IDs to parallel arrays {ts: [...], vs: [...], cis: [...]}. */
        JSONObject getConceptData() {
            JSONObject dump = new JSONObject();
            for (String uuid : mConceptsToDump) {
                try {
                    int index = 0;
                    JSONArray ts = new JSONArray();
                    JSONArray vs = new JSONArray();
                    JSONArray cis = new JSONArray();
                    for (Column column : mColumnsByStartMillis.values()) {
                        SortedSet<ObsPoint> points = column.pointSetByConceptUuid.get(uuid);
                        if (points != null && points.size() > 0) {
                            for (ObsPoint point : points) {
                                ts.put(point.time.getMillis());
                                vs.put(point.value.number); // TODO: handle other types
                                cis.put(index);
                            }
                        }
                        index++;
                    }
                    JSONObject conceptInfo = new JSONObject();
                    conceptInfo.put("ts", ts);
                    conceptInfo.put("vs", vs);
                    conceptInfo.put("cis", cis);
                    dump.put("" + Utils.compressUuid(uuid), conceptInfo);
                } catch (JSONException e) {
                    LOG.e(e, "JSON error while dumping chart data");
                }
            }
            return dump;
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
    }
}
