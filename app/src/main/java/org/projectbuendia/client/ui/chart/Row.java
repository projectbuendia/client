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
        DEFAULTS.put("row_section", new ChartItem("", "", true, null, "", "", "", "", ""));
        DEFAULTS.put("select_one", new ChartItem("", "", false, null, "{1,abbr}", "{1,name}", "", "", ""));
        DEFAULTS.put("yes_no", new ChartItem("", "", false, null, "{1,yes_no,\u25cf;\u25cb}", "{1,yes_no,Yes;No}", "", "{1,yes_no,color:black;color:DarkGray}", ""));
        DEFAULTS.put("yes_no_unknown", new ChartItem("", "", false, null, "{1,yes_no_unknown,\u25cf;\u25cb;?}", "{1,yes_no,Yes;No;Unknown}", "", "{1,yes_no,color:black;color:DarkGray}", ""));
        DEFAULTS.put("number", new ChartItem("", "", false, null, "0", "0", "", "", ""));
        DEFAULTS.put("text", new ChartItem("", "", false, null, "{1,text,5}", "{1,text,40}", "", "", ""));
        DEFAULTS.put("date", new ChartItem("", "", false, null, "{1,date,MMM dd}", "{1,date,MMM dd}", "", "", ""));
        DEFAULTS.put("time", new ChartItem("", "", false, null, "{1,time,HH:mm}", "{1,time,HH:mm}", "", "", ""));
        DEFAULTS.put("severity_bars", new ChartItem("", "", false, null, "{1,select,1107:\u25cb;1498:\u002d;1499:\u003d;1500:\u2261}", "{1,name}", "{1,select,1500:critical}", "{1,select,1107:color:DarkGray}", ""));
    }

    public Row(@Nonnull ChartItem item) {
        this.item = item.withDefaults(DEFAULTS.get(item.type));
    }
}
