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
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.utils.Logger;

import java.io.IOException;
import java.net.UnknownHostException;

/** A {@link HealthCheck} that checks whether the package server is up and running. */
public class PackageServerHealthCheck extends HealthCheck {

    private static final Logger LOG = Logger.create();

    /** Check for issues with this frequency. */
    private static final int CHECK_PERIOD_MS = 20000;

    private static final String HEALTH_CHECK_ENDPOINT = "/dists/stable/Release";

    private final Object mLock = new Object();

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private AppSettings mSettings;
    private final Runnable mHealthCheckRunnable = new Runnable() {

        @Override
        public void run() {
            performCheck();

            synchronized (mLock) {
                if (mHandler != null) {
                    mHandler.postDelayed(this, CHECK_PERIOD_MS);
                }
            }
        }

        private void performCheck() {
            Uri uri = Uri.parse(mSettings.getPackageServerUrl(HEALTH_CHECK_ENDPOINT));
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet(uri.toString());
            if (getRequest.getURI().getHost() == null) {
                LOG.w("Configured package server URL is invalid: %s", uri);
                reportIssue(HealthIssue.SERVER_CONFIGURATION_INVALID);
                return;
            }

            try {
                HttpResponse response = httpClient.execute(getRequest);
                switch (response.getStatusLine().getStatusCode()) {
                    case HttpStatus.SC_OK:
                        LOG.d("Package server check completed, OK.");
                        resolveAllIssues();
                        return;
                    case HttpStatus.SC_NOT_FOUND:
                        LOG.d("Package server check completed, 404.");
                        // The package server is reachable if we get a 404.
                        resolveIssue(HealthIssue.PACKAGE_SERVER_HOST_UNREACHABLE);
                        reportIssue(HealthIssue.PACKAGE_SERVER_INDEX_NOT_FOUND);
                        return;
                    default:
                        LOG.w("Package server check failed for URI %1$s.", uri);
                }
            } catch (UnknownHostException | IllegalArgumentException e) {
                LOG.w("Package server unreachable: %s", uri);
                reportIssue(HealthIssue.PACKAGE_SERVER_HOST_UNREACHABLE);
            } catch (HttpHostConnectException e) {
                LOG.w("Package server connection refused: %s", e.getHost());
            } catch (IOException e) {
                LOG.w(e, "Package server check failed: %s", uri);
            }
        }
    };

    PackageServerHealthCheck(Application application, AppSettings settings) {
        super(application);
        mSettings = settings;
    }

    @Override
    protected void startImpl() {
        synchronized (mLock) {
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread("Buendia Package Server Health Check");
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper());
                mHandler.post(mHealthCheckRunnable);
            }
        }
    }

    @Override
    protected void stopImpl() {
        synchronized (mLock) {
            if (mHandlerThread != null) {
                mHandlerThread.quit();
                mHandlerThread = null;
                mHandler = null;
            }
        }
    }
}
