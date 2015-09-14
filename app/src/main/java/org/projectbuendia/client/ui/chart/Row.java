package org.projectbuendia.client.ui.chart;

import org.projectbuendia.client.models.Concepts;
import org.projectbuendia.client.net.json.ConceptType;

import java.util.HashMap;
import java.util.Map;

/** Descriptor for a row (observed attribute) in the patient history grid. */
public class Row {
    public String conceptUuid;
    public String heading;
    public String type;
    public String format;

    static Map<String, String> CONCEPT_FORMATS = new HashMap<>();
    static {
        CONCEPT_FORMATS.put(Concepts.TEMPERATURE_UUID, "##.#");
        CONCEPT_FORMATS.put(Concepts.WEIGHT_UUID, "##.#");
        CONCEPT_FORMATS.put(Concepts.PULSE_UUID, "##");
        CONCEPT_FORMATS.put(Concepts.RESPIRATION_UUID, "##");
    }

    static Map<Value.Type, String> DEFAULT_FORMATS = new HashMap<>();
    static {
        DEFAULT_FORMATS.put(Value.Type.CODED, "{1,abbr}");
        DEFAULT_FORMATS.put(Value.Type.DATE, "{1,date,dd MMM}");
        DEFAULT_FORMATS.put(Value.Type.NUMERIC, "###");
        DEFAULT_FORMATS.put(Value.Type.TEXT, "{1}");
        DEFAULT_FORMATS.put(Value.Type.BOOLEAN, "{1,yes_no,\u25cf}");
    }

    public Row(String conceptUuid, String heading, String type) {
        this.conceptUuid = conceptUuid;
        this.heading = heading;
        this.type = type;
        this.format = CONCEPT_FORMATS.get(conceptUuid);
        if (format == null) format = DEFAULT_FORMATS.get(Value.getValueType(conceptUuid, ConceptType.NONE, null));
    }
}