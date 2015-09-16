package org.projectbuendia.client.ui.chart;

import org.projectbuendia.client.models.ChartItem;
import org.projectbuendia.client.sync.LocalizedObs;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

/** Descriptor for a tile (latest observed value) in the patient chart. */
public class Tile {
    public final ChartItem item;
    public final LocalizedObs[] obsValues;

    static Map<String, ChartItem> DEFAULTS = new HashMap<>();
    static {
        DEFAULTS.put("select_one", new ChartItem("", "", false, null, "{1,abbr}", "{1,name}", "", ""));
        DEFAULTS.put("yes_no", new ChartItem("", "", false, null, "{1,yes_no,Yes;No}", "", "", ""));
        DEFAULTS.put("number", new ChartItem("", "", false, null, "0", "", "", ""));
        DEFAULTS.put("text", new ChartItem("", "", false, null, "{1,text,60}", "", "", ""));
        DEFAULTS.put("date", new ChartItem("", "", false, null, "{1,date,YYYY-MM-dd}", "", "", ""));
        DEFAULTS.put("time", new ChartItem("", "", false, null, "{1,time,HH:mm}", "", "", ""));
        DEFAULTS.put("obs_date", new ChartItem("", "", false, null, "{1,obs_time,YYYY-MM-dd}", "", "", ""));
        DEFAULTS.put("obs_time", new ChartItem("", "", false, null, "{1,obs_time,HH:mm}", "", "", ""));
    }
    
    public Tile(@Nonnull ChartItem item, @Nonnull LocalizedObs[] obsValues) {
        this.item = item.withDefaults(DEFAULTS.get(item.type));
        this.obsValues = obsValues;
    }
}
