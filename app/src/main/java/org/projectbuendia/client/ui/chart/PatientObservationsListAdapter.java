package org.projectbuendia.client.ui.chart;

import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.Observations;

import java.util.Date;

/**
 * A {@link android.widget.ListAdapter} that displays observations for a given patient, matching a
 * given concept UUID.
 * <p>
 * TODO: This adapter currently does some database queries on the main thread - we should
 * offload these to a background thread for performance reasons.
 */
public class PatientObservationsListAdapter extends CursorAdapter {

    private static final String[] PROJECTION = new String[] {
            "rowid AS _id",
            Observations.ENTERER_UUID,
            Observations.ENCOUNTER_MILLIS,
            Observations.VALUE,
    };

    private final ContentResolver mContentResolver;

    public PatientObservationsListAdapter(Context context) {
        super(context, null, 0);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.notes_list_adapter_note_template, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Obtain data from cursor
        Date encounterTimestamp = new Date(cursor.getLong(
                cursor.getColumnIndexOrThrow(Observations.ENCOUNTER_MILLIS)));
        String value = cursor.getString(
                cursor.getColumnIndexOrThrow(Observations.VALUE));
        String entererUuid = cursor.getString(
                cursor.getColumnIndexOrThrow(Observations.ENTERER_UUID));
        String enterer = getUsersNameFromUuid(entererUuid);

        // Obtain view references
        ViewGroup viewGroup = (ViewGroup) view;
        TextView metaLine = (TextView) viewGroup.findViewById(R.id.meta);
        TextView content = (TextView) viewGroup.findViewById(R.id.observation_content);

        // Set content
        metaLine.setText(context.getResources().getString(
                enterer == null
                        ? R.string.notes_list_metadata_format_no_user_info
                        : R.string.notes_list_metadata_format,
                encounterTimestamp, enterer));
        content.setText(value);
    }

    /**
     * Returns the users' full name from a UUID. Note that this performs a database query, and so
     * ideally calls should be kept off the main thread. It does not perform a network request to
     * check for new users on the server.
     *
     * @param uuid The uuid of the user whose name to return. Note that if {@code null} is passed,
     *             {@code null} will be returned.
     * @return the users' name, if a user was found matching this UUID. {@code null} otherwise.
     */
    public @Nullable String getUsersNameFromUuid(@Nullable String uuid) {
        if (uuid == null) {
            return null;
        }
        try (Cursor cursor = mContentResolver.query(
                Contracts.Users.CONTENT_URI.buildUpon().appendPath(uuid).build(),
                new String[]{Contracts.Users.FULL_NAME},
                null,
                null,
                null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                // Either there wasn't a cursor, or the result set was empty.
                // This is a user we don't know about.
                return null;
            }
            return cursor.getString(0);
        }
    }

    public static class ObservationsListLoaderCallbacks
            implements LoaderManager.LoaderCallbacks<Cursor> {

        private final Context mContext;
        private final String mPatientUuid;
        private final String mConceptUuid;
        private final CursorAdapter mAdapter;

        public ObservationsListLoaderCallbacks(
                Context context, String patientUuid, String conceptUuid, CursorAdapter adapter) {
            mContext = context;
            mPatientUuid = patientUuid;
            mConceptUuid = conceptUuid;
            mAdapter = adapter;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(mContext,
                    Observations.CONTENT_URI,
                    PROJECTION,
                    Observations.PATIENT_UUID + " = ? AND "
                            + Observations.CONCEPT_UUID + " = ? ",
                    new String[]{mPatientUuid, mConceptUuid},
                    Observations.ENCOUNTER_MILLIS);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.swapCursor(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }
    }
}
