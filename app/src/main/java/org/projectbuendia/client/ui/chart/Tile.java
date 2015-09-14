package org.projectbuendia.client.ui.chart;

import org.projectbuendia.client.models.Concepts;
import org.projectbuendia.client.net.json.ConceptType;
import org.projectbuendia.client.sync.LocalizedObs;

import java.util.HashMap;
import java.util.Map;

/** Descriptor for a tile (latest observed value) in the patient chart. */
public class Tile {
    public String conceptUuid;
    public String heading;
    public LocalizedObs obs;
    public String format;
    public String captionFormat;


    static Map<String, String> CONCEPT_FORMATS = new HashMap<>();
    static {
        CONCEPT_FORMATS.put(Concepts.TEMPERATURE_UUID, "##.#° C");
        CONCEPT_FORMATS.put(Concepts.WEIGHT_UUID, "##.# kg");
        CONCEPT_FORMATS.put(Concepts.PULSE_UUID, "## bpm");
        CONCEPT_FORMATS.put(Concepts.RESPIRATION_UUID, "## bpm");
    }

    static Map<Value.Type, String> DEFAULT_FORMATS = new HashMap<>();
    static {
        DEFAULT_FORMATS.put(Value.Type.CODED, "{1,abbr}");
        DEFAULT_FORMATS.put(Value.Type.DATE, "{1,date,dd MMM}");
        DEFAULT_FORMATS.put(Value.Type.NUMERIC, "###");
        DEFAULT_FORMATS.put(Value.Type.TEXT, "{1}");
        DEFAULT_FORMATS.put(Value.Type.BOOLEAN, "{1,yes_no,Yes;No}");
    }

    static Map<Value.Type, String> DEFAULT_CAPTION_FORMATS = new HashMap<>();
    static {
        DEFAULT_CAPTION_FORMATS.put(Value.Type.CODED, "{1,name}");
        DEFAULT_CAPTION_FORMATS.put(Value.Type.DATE, "");
        DEFAULT_CAPTION_FORMATS.put(Value.Type.NUMERIC, "");
        DEFAULT_CAPTION_FORMATS.put(Value.Type.TEXT, "");
        DEFAULT_CAPTION_FORMATS.put(Value.Type.BOOLEAN, "");
    }

    public Tile(String conceptUuid, String heading, LocalizedObs obs) {
        this.conceptUuid = conceptUuid;
        this.heading = heading;
        this.obs = obs;
        Value.Type type = Value.getValueType(conceptUuid, ConceptType.NONE, null);
        this.format = CONCEPT_FORMATS.get(conceptUuid);
        if (format == null) format = DEFAULT_FORMATS.get(type);
        this.captionFormat = DEFAULT_CAPTION_FORMATS.get(type);
    }
}