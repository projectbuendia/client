package org.msf.records.sync;

import android.content.ContentProviderOperation;
import android.content.SyncResult;
import android.util.Log;

import org.msf.records.model.Concept;
import org.msf.records.model.ConceptList;

import java.util.ArrayList;
import java.util.Map;

import static org.msf.records.sync.ChartProviderContract.CONCEPT_NAMES_CONTENT_URI;

/**
 * A helper class for turning the Java beans that are the result of chart RPC calls into
 * appropriate ContentProviderOperations for inserting into the DB.
 */
public class ChartRpcToDb {

    private static final String TAG = "ChartRpcToDb";

    /**
     * Convert a concept response into appropriate inserts in the concept and concept_name tables.
     */
    public static ArrayList<ContentProviderOperation> conceptRpcToDb(ConceptList response,
                                                                     SyncResult syncResult) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        for (Concept concept : response.results) {
            // This is safe because we have implemented insert on the content provider
            // with replace.
            operations.add(ContentProviderOperation
                    .newUpdate(ChartProviderContract.CONCEPTS_CONTENT_URI)
                    .withValue(ChartProviderContract.ChartColumns.CONCEPT_UUID, concept.uuid)
                    .withValue(ChartProviderContract.ChartColumns.CONCEPT_TYPE, concept.type.name())
                    .build());
            syncResult.stats.numInserts++;
            for (Map.Entry<String, String> entry : concept.names.entrySet()) {
                String locale = entry.getKey();
                if (locale == null) {
                    Log.e(TAG, "null locale in concept name rpc for " + concept);
                    continue;
                }
                String name = entry.getValue();
                if (name == null) {
                    Log.e(TAG, "null name in concept name rpc for " + concept);
                    continue;
                }
                operations.add(ContentProviderOperation
                        .newInsert(CONCEPT_NAMES_CONTENT_URI)
                        .withValue(ChartProviderContract.ChartColumns.CONCEPT_UUID, concept.uuid)
                        .withValue(ChartProviderContract.ChartColumns.LOCALE, locale)
                        .withValue(ChartProviderContract.ChartColumns.NAME, name)
                        .build());
                syncResult.stats.numInserts++;
            }
        }
        return operations;
    }
}
