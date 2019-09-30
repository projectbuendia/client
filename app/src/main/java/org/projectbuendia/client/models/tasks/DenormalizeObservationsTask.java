// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.models.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;

import com.google.common.collect.ImmutableSet;

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemLoadFailedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.events.data.ItemUpdatedEvent;
import org.projectbuendia.client.events.data.PatientUpdateFailedEvent;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.Set;

public class DenormalizeObservationsTask extends AsyncTask<Void, Void, PatientUpdateFailedEvent> {
    private static final Logger LOG = Logger.create();
    private final TaskFactory mTaskFactory;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final String mPatientUuid;
    private final CrudEventBus mBus;

    private static final String DENORMALIZE_QUERY = ""
        + "UPDATE patients SET %s = ("
        + "    SELECT obs.value"
        + "    FROM observations AS obs"
        + "    WHERE observations.voided IS NOT 1"
        + "        AND observations.patient_uuid = patients.patient_uuid"
        + "        AND observations.concept_uuid = ?"
        + "    ORDER BY observations.encounter_millis DESCENDING"
        + "    LIMIT 1"
        + ") where patients.patient_uuid = ?";

    private static final Set<String> DENORMALIZED_CONCEPTS = ImmutableSet.of(
        ConceptUuids.PREGNANCY_UUID,
        ConceptUuids.PLACEMENT_UUID
    );

    DenormalizeObservationsTask(
        TaskFactory taskFactory,
        Server server,
        ContentResolver contentResolver,
        String patientUuid,
        CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mServer = server;
        mContentResolver = contentResolver;
        mPatientUuid = patientUuid;
        mBus = bus;
    }

    @Override protected PatientUpdateFailedEvent doInBackground(Void... params) {
        String pregnancyValue = getLatestValue(mPatientUuid, ConceptUuids.PREGNANCY_UUID);
        String placementValue = getLatestValue(mPatientUuid, ConceptUuids.PLACEMENT_UUID);

        LOG.i("Denormalizing observations for patient %s (%s, %s)", mPatientUuid, pregnancyValue, placementValue);
        ContentValues cv = new ContentValues();
        if (pregnancyValue != null) {
            cv.put(Patients.PREGNANCY, ConceptUuids.isYes(pregnancyValue));
        }
        if (placementValue != null) {
            String[] parts = Utils.splitFields(placementValue, "/", 2);
            cv.put(Patients.LOCATION_UUID, parts[0]);
            cv.put(Patients.BED_NUMBER, parts[1]);
        }
        if (cv.size() == 0) return null;  // no update needed

        int count = mContentResolver.update(
            Patients.URI, cv, Patients.UUID + " = ?", new String[] {mPatientUuid}
        );
        if (count == 1) return null;

        // If the patient doesn't exist, that's okay.  It's possible for observations
        // about a patient to arrive before a newly created patient arrives.
        return null;
    }

    private String getLatestValue(String patientUuid, String conceptUuid) {
        try (Cursor cursor = mContentResolver.query(
            Observations.URI, new String[] {Observations.VALUE},
            Observations.PATIENT_UUID + " = ?" +
                " AND " + Observations.CONCEPT_UUID + " = ?" +
                " AND " + Observations.VOIDED + " IS NOT 1",
            new String[] {patientUuid, conceptUuid},
            Observations.MILLIS + " DESC"
        )) {
            return cursor.moveToNext() ? cursor.getString(0) : null;
        }
    }

    @Override protected void onPostExecute(PatientUpdateFailedEvent event) {
        if (event != null) mBus.post(event);
        else {
            mBus.register(new EventSubscriber());
            mTaskFactory.newLoadItemTask(
                Patients.URI, null, new UuidFilter(), mPatientUuid, Patient::load, mBus
            ).execute();
        }
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class EventSubscriber {
        public void onEventMainThread(ItemLoadedEvent<?> event) {
            if (event.item instanceof Patient) {
                mBus.post(new ItemUpdatedEvent<>(mPatientUuid, (Patient) event.item));
                mBus.unregister(this);
            }
        }

        public void onEventMainThread(ItemLoadFailedEvent event) {
            mBus.post(new PatientUpdateFailedEvent(
                PatientUpdateFailedEvent.REASON_CLIENT, new Exception(event.error)));
            mBus.unregister(this);
        }
    }

    public static boolean needsDenormalization(String conceptUuid) {
        return DENORMALIZED_CONCEPTS.contains(conceptUuid);
    }

    public static boolean needsDenormalization(ContentValues[] values) {
        for (ContentValues cv  : values) {
            if (DENORMALIZED_CONCEPTS.contains(
                cv.getAsString(Observations.CONCEPT_UUID))) return true;
        }
        return false;
    }
}
