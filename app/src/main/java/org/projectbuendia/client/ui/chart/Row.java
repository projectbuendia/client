package org.projectbuendia.client.ui.chart;

import org.projectbuendia.client.models.ChartItem;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/** Descriptor for a row (observed attribute) in the patient history grid. */
public class Row {
    public ChartItem item;

    static Map<String, ChartItem> DEFAULTS = new HashMap<>();
    static {
        DEFAULTS.put("select_one", new ChartItem("", "", false, null, "{1,abbr}", "{1,name}", "", ""));
        DEFAULTS.put("yes_no", new ChartItem("", "", false, null, "{1,yes_no,\u25cf}", "{1,yes_no,Yes;No}", "", ""));
        DEFAULTS.put("number", new ChartItem("", "", false, null, "0", "0", "", ""));
        DEFAULTS.put("text", new ChartItem("", "", false, null, "{1,text,5}", "{1,text,40}", "", ""));
        DEFAULTS.put("date", new ChartItem("", "", false, null, "{1,date,MMM dd}", "{1,date,MMM dd}", "", ""));
        DEFAULTS.put("time", new ChartItem("", "", false, null, "{1,time,HH:mm}", "{1,time,HH:mm}", "", ""));
        DEFAULTS.put("obs_date", new ChartItem("", "", false, null, "{1,obs_time,MMM dd}", "{1,obs_time,MMM dd}", "", ""));
        DEFAULTS.put("obs_time", new ChartItem("", "", false, null, "{1,obs_time,HH:mm}", "{1,obs_time,MMM dd 'at' HH:mm}", "", ""));
    }

    public Row(@Nonnull ChartItem item) {
        this.item = item.withDefaults(DEFAULTS.get(item.type));
    }
}
