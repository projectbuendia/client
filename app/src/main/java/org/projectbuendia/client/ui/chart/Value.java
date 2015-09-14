package org.projectbuendia.client.ui.chart;

import android.support.annotation.NonNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.projectbuendia.client.models.Concepts;
import org.projectbuendia.client.net.json.ConceptType;
import org.projectbuendia.client.sync.LocalizedChartHelper;
import org.projectbuendia.client.sync.LocalizedObs;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * The observed value for a concept, broken out into fields of various types
 * and representations to facilitate rendering in a template.
 */
class Value implements Comparable<Value> {
    public static ObsTimeComparator BY_OBS_TIME = new ObsTimeComparator();
    public DateTime observed;  // when the observation was taken
    public boolean present;  // true if a value of any type is present
    public Double number;  // number for a numeric value (type "Numeric")
    public String text;  // text of a free text value (type "Text")
    public String uuid;  // UUID identifying a coded value (type "Coded")
    public String name;  // localized name of a coded value (type "Coded")
    public String abbrev;  // abbreviation for a coded value (type "Coded")
    // false for a coded value (type "Coded") that signifies a lack of a
    // symptom (condition normal); true for any other coded value
    public Boolean bool;  // true or false for a boolean value (type "Boolean");
    static final int MAX_ABBREV_CHARS = 3;

    public Value(LocalizedObs obs, Chronology chronology) {
        final Set<String> FALSE_CODED_VALUES = new ImmutableSet.Builder<String>().add(
            Concepts.UNKNOWN_UUID).addAll(LocalizedChartHelper.NO_SYMPTOM_VALUES).build();
        if (obs == null) {
            observed = null;
            present = false;
            return;
        }
        observed = obs.encounterTime;
        present = obs.value != null;
        if (present) {
            switch (getConceptType(obs.conceptUuid, obs.conceptType, obs.value)) {
                case NUMERIC:
                    number = Double.valueOf(obs.value);
                    break;
                case TEXT:
                    text = obs.value;
                    break;
                case CODED:
                    uuid = obs.value;
                    name = obs.localizedValue;
                    int abbrevLength = name.indexOf('.');
                    if (abbrevLength >= 1 && abbrevLength <= MAX_ABBREV_CHARS) {
                        abbrev = name.substring(0, abbrevLength);
                        name = name.substring(abbrevLength + 1).trim();
                    } else {
                        abbrev = name.substring(0, MAX_ABBREV_CHARS) + "\u2026";
                    }
                    // fall through to set bool as well
                case BOOLEAN:
                    bool = !FALSE_CODED_VALUES.contains(obs.value);
                    break;
            }
            // TODO: Date / DateTime values (this.date, this.utc, this.local)
        }
    }

    static Type getConceptType(String conceptUuid, ConceptType conceptType, String value) {
        final Set<String> CODED_CONCEPTS = ImmutableSet.of(
            Concepts.GENERAL_CONDITION_UUID,
            Concepts.RESPONSIVENESS_UUID,
            Concepts.MOBILITY_UUID,
            Concepts.PAIN_UUID,
            Concepts.WEAKNESS_UUID);
        final Set<String> NUMERIC_CONCEPTS = ImmutableSet.of(
            Concepts.TEMPERATURE_UUID,
            Concepts.VOMITING_UUID,
            Concepts.DIARRHEA_UUID,
            Concepts.WEIGHT_UUID);
        final Set<String> TEXT_CONCEPTS = ImmutableSet.of(
            Concepts.NOTES_UUID);
        final Set<String> BOOLEAN_ANSWERS = ImmutableSet.of(
            Concepts.YES_UUID,
            Concepts.NO_UUID,
            Concepts.UNKNOWN_UUID);

        if (NUMERIC_CONCEPTS.contains(conceptUuid)) return Type.NUMERIC;
        if (TEXT_CONCEPTS.contains(conceptUuid)) return Type.TEXT;
        if (CODED_CONCEPTS.contains(conceptUuid)) return Type.CODED;
        if (BOOLEAN_ANSWERS.contains(value)) return Type.BOOLEAN;
        switch (conceptType) {
            case CODED:
                return Type.CODED;
            case NUMERIC:
                return Type.NUMERIC;
            case TEXT:
                return Type.TEXT;
        }
        return Type.BOOLEAN;
    }

    /**
     * Compares value instances according to a total ordering such that:
     * - The empty value (present == false) is ordered before all others.
     * - The Boolean value false is ordered before all other values and types.
     * - Numeric values are ordered from least to greatest magnitude.
     * - Text values are ordered lexicographically from A to Z.
     * - Coded values are ordered from least severe to most severe (if they can
     * be interpreted as having a severity); or from first to last (if they can
     * be interpreted as having a typical temporal sequence).
     * - The Boolean value true is ordered after all other values and types.
     * @param other The other Value to compare to.
     * @return
     */
    @Override public int compareTo(@NonNull Value other) {
        if (bool != null && other.bool != null) {
            return bool.compareTo(other.bool);
        }
        if (number != null && other.number != null) {
            return number.compareTo(other.number);
        }
        if (text != null && other.text != null) {
            return text.compareTo(other.text);
        }
        if (uuid != null && other.uuid != null) {
            return getCodedValueOrdering().compareTo(other.getCodedValueOrdering());
        }
        return getTypeOrdering().compareTo(other.getTypeOrdering());
    }

    /**
     * Gets a number specifying the ordering of coded values.  These are
     * arranged from least to most severe so that using the Pebble "max" filter
     * will select the most severe value from a list of values.
     */
    public Integer getCodedValueOrdering() {
        final Map<String, Integer> CODED_VALUE_ORDERING = new ImmutableMap.Builder<String, Integer>()
            .put(Concepts.NO_UUID, 0)
            .put(Concepts.NONE_UUID, 1)
            .put(Concepts.NORMAL_UUID, 2)
            .put(Concepts.SOLID_FOOD_UUID, 3)
            .put(Concepts.MILD_UUID, 4)
            .put(Concepts.MODERATE_UUID, 5)
            .put(Concepts.SEVERE_UUID, 6)
            .put(Concepts.YES_UUID, 7).build();
        Integer cvo = CODED_VALUE_ORDERING.get(uuid);
        return cvo == null ? 0 : cvo;
    }

    /** Gets a number specifying the ordering of Values of different types. */
    public Integer getTypeOrdering() {
        if (bool != null) return bool ? 5 : 1;
        if (number != null) return 2;
        if (text != null) return 3;
        if (uuid != null) return 4;
        return 0;
    }

    /** A comparator that orders values from first to last observation time. */
    public static class ObsTimeComparator implements Comparator<Value> {
        @Override public int compare(Value left, Value right) {
            return left.observed.compareTo(right.observed);
        }
    }

    ;

    enum Type {
        NUMERIC,
        TEXT,
        CODED,
        BOOLEAN
    }
}
