package org.projectbuendia.client.ui.chart;

/** Descriptor for a tile (latest observed value) in the patient chart. */
public class Tile {
    public String conceptUuid;
    public String heading;
    public Value value;

    public Tile(String conceptUuid, String heading, Value value) {
        this.conceptUuid = conceptUuid;
        this.heading = heading;
        this.value = value;
    }
}