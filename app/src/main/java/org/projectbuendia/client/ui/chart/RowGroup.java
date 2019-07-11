package org.projectbuendia.client.ui.chart;

import java.util.ArrayList;
import java.util.List;

/** Descriptor for a group of rows in the patient history grid. */
public class RowGroup {
    public String title;
    public List<Row> rows;

    public RowGroup(String title) {
        this.title = title;
        this.rows = new ArrayList<>();
    }
}
