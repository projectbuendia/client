package org.msf.records.updater;

import android.os.Handler;
import android.os.Looper;

import com.android.volley.Response;

import org.msf.records.model.UpdateInfo;
import org.msf.records.net.VolleySingleton;

import java.util.ArrayList;
import java.util.List;

/**
 * A fake {@link UpdateServer} for testing.
 */
public class FakeUpdateServer extends UpdateServer {

    /**
     * A {@link Handler} used for issuing responses on the main thread, which is Volley's default
     * behavior.
     */
    private final Handler mHandler;

    private final List<UpdateInfo> mUpdateInfo;

    public FakeUpdateServer(VolleySingleton volleySingleton) {
        super(volleySingleton, null /*rootUrl*/);
        mHandler = new Handler(Looper.getMainLooper());
        mUpdateInfo = new ArrayList<>();
        UpdateInfo latestUpdateInfo = new UpdateInfo();
        latestUpdateInfo.url = "http://www.example.org/foo.apk";
        latestUpdateInfo.version = "0.1.0";
        mUpdateInfo.add(latestUpdateInfo);
    }

    @Override
    public void getAndroidUpdateInfo(
            final Response.Listener<List<UpdateInfo>> listener,
            final Response.ErrorListener errorListener) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                listener.onResponse(mUpdateInfo);
            }
        });
    }
}
