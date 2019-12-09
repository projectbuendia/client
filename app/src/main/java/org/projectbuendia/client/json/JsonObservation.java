package org.projectbuendia.client.json;

import android.content.ContentValues;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.projectbuendia.models.Obs;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.utils.Utils;

/**
 * An plain-object representation of an observation received from the server.
 */
public class JsonObservation {
    public String uuid;
    public String encounter_uuid;
    public String patient_uuid;
    public String provider_uuid;
    public String concept_uuid;
    public Datatype type;
    public DateTime time;
    public String order_uuid;
    public String value_coded;
    public Double value_numeric;
    public String value_text;
    public LocalDate value_date;
    public Instant value_datetime;
    public boolean voided;

    public JsonObservation(Obs obs) {
        this.uuid = obs.uuid;
        this.encounter_uuid = obs.encounterUuid;
        this.patient_uuid = obs.patientUuid;
        this.provider_uuid = obs.providerUuid;
        this.concept_uuid = obs.conceptUuid;
        this.type = obs.type;
        this.time = obs.time;
        this.order_uuid = obs.orderUuid;
        if (obs.value != null) {
            if (obs.type == Datatype.CODED) this.value_coded = obs.value;
            if (obs.type == Datatype.NUMERIC) this.value_numeric = Double.valueOf(obs.value);
            if (obs.type == Datatype.TEXT) this.value_text = obs.value;
            if (obs.type == Datatype.DATE) this.value_date = Utils.toLocalDate(obs.value);
            if (obs.type == Datatype.DATETIME) this.value_datetime = new Instant(Long.valueOf(obs.value));
        }
    }

    public String getValueAsString() {
        switch (type) {
            case CODED:
                return value_coded;
            case NUMERIC:
                return "" + value_numeric;
            case TEXT:
                return value_text;
            case DATE:
                return value_date.toString();
            case DATETIME:
                return "" + value_datetime.getMillis();
        }
        return "";
    }

    /** Converts an observation into ContentValues for the encounters table. */
    public ContentValues toContentValues() {
        ContentValues cvs = new ContentValues();
        cvs.put(Observations.UUID, uuid);
        cvs.put(Observations.ENCOUNTER_UUID, encounter_uuid);
        cvs.put(Observations.PATIENT_UUID, patient_uuid);
        cvs.put(Observations.PROVIDER_UUID, provider_uuid);
        cvs.put(Observations.CONCEPT_UUID, concept_uuid);
        cvs.put(Observations.TYPE, type != null ? type.name() : Datatype.NONE.name());
        cvs.put(Observations.MILLIS, time.getMillis());
        cvs.put(Observations.ORDER_UUID, order_uuid);
        cvs.put(Observations.VALUE, getValueAsString());
        return cvs;
    }
}
