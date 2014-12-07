package org.msf.records.updater.testing;

// TODO(dxchen): Move all testing stuff into a separate module that doesn't build in release builds.

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.android.volley.Response;

import org.msf.records.model.UpdateInfo;
import org.msf.records.updater.UpdateServer;

import java.util.Arrays;

/**
 * A fake {@link UpdateServer} for testing.
 */
public class FakeUpdateServer extends UpdateServer {

    /**
     * A {@link Handler} used for issuing responses on the main thread, which is Volley's default
     * behavior.
     */
    private final Handler mHandler;

    private final UpdateInfo mUpdateInfo;

    public FakeUpdateServer() {
        super(null /*rootUrl*/);
        mHandler = new Handler(Looper.getMainLooper());
        mUpdateInfo = new UpdateInfo();
        mUpdateInfo.androidClient = new UpdateInfo.ComponentUpdateInfo();
        mUpdateInfo.androidClient.latestVersionString = "0.1.0";
        mUpdateInfo.androidClient.installWindowHoursMin = 200;
        mUpdateInfo.androidClient.installWindowHoursMax = 400;
        mUpdateInfo.androidClient.run = Arrays.asList("foo/bar/baz.apk");
    }

    @Override
    public void getAndroidUpdateInfo(
            final Response.Listener<UpdateInfo> listener,
            final Response.ErrorListener errorListener,
            @Nullable String tag) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                listener.onResponse(mUpdateInfo);
            }
        });
    }
}
