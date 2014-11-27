package org.msf.records.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.msf.records.App;
import org.msf.records.model.ConceptList;
import org.msf.records.net.OpenMrsChartServer;

/**
 * Created by nfortescue on 11/26/14.
 */
public class ChartSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "ChartSyncAdapter";
    /**
     * Content resolver, for performing database operations.
     */
    private final ContentResolver mContentResolver;

    public ChartSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    public ChartSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              final ContentProviderClient provider,
                              final SyncResult syncResult) {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        chartServer.getConcepts(new Response.Listener<ConceptList>() {
            @Override
            public void onResponse(ConceptList response) {
                try {
                    provider.applyBatch(ChartRpcToDb.conceptRpcToDb(response));
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Failed to sync concepts", error);
            }
        });
    }

}
