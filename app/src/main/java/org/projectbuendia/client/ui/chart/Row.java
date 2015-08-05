package org.projectbuendia.client.ui.chart;

/** Descriptor for a row (observed attribute) in the patient history grid. */
public class Row {
    public String conceptUuid;
    public String heading;

    public Row(String conceptUuid, String heading) {
        this.conceptUuid = conceptUuid;
        this.heading = heading;
    }
}