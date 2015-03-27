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

package org.msf.records.updater;

import android.os.Handler;
import android.os.Looper;

import com.android.volley.Response;

import org.msf.records.model.UpdateInfo;
import org.msf.records.net.VolleySingleton;

import java.util.ArrayList;
import java.util.List;

/** A fake {@link UpdateServer} for testing. */
public class FakeUpdateServer extends UpdateServer {

    /**
     * A {@link Handler} used for issuing responses on the main thread, which is Volley's default
     * behavior.
     */
    private final Handler mHandler;

    private final List<UpdateInfo> mUpdateInfo;

    /**
     * Instantiates a {@link FakeUpdateServer} using the given {@link VolleySingleton}.
     * @param volleySingleton the {@link VolleySingleton}
     */
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
    public void getPackageIndex(
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
