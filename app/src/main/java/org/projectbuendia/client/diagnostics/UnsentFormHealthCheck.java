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

package org.projectbuendia.client.diagnostics;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.models.UnsentForm;
import org.projectbuendia.client.ui.OdkActivityLauncher;
import org.projectbuendia.client.utils.Logger;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

/** A {@link HealthCheck} that checks whether there are saved forms which are not submitted and need
 * to be resubmitted to the server. */
public class UnsentFormHealthCheck extends HealthCheck {

    private static final Logger LOG = Logger.create();

    /** Check for issues with this frequency. */
    private static final int CHECK_PERIOD_MS = 10000;

    private final Object mLock = new Object();

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private final Runnable mHealthCheckRunnable = new Runnable() {

        @Override public void run() {
            performCheck();

            synchronized (mLock) {
                if (mHandler != null) {
                    mHandler.postDelayed(this, CHECK_PERIOD_MS);
                }
            }
        }

        private void performCheck() {
            final List<UnsentForm> forms = OdkActivityLauncher.getUnsetForms(App.getInstance()
                .getContentResolver());

            if(!forms.isEmpty()) {
                LOG.w("There are %d unsent forms saved locally and need to be resent to the server",
                    forms.size());
                reportIssue(HealthIssue.PENDING_FORM_SUBMISSION);
                return;
            } else {
                resolveIssue(HealthIssue.PENDING_FORM_SUBMISSION);
            }
        }
    };

    UnsentFormHealthCheck(Application application) {
        super(application);
    }

    @Override protected void startImpl() {
        synchronized (mLock) {
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread("Buendia Unsent Form Health Check");
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper());
                mHandler.post(mHealthCheckRunnable);
            }
        }
    }

    @Override protected void stopImpl() {
        synchronized (mLock) {
            if (mHandlerThread != null) {
                mHandlerThread.quit();
                mHandlerThread = null;
                mHandler = null;
            }
        }
    }
}
