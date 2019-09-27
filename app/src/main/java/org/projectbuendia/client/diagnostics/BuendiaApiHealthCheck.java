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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.apache.http.HttpResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.net.OpenMrsConnectionDetails;
import org.projectbuendia.client.utils.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link HealthCheck} that checks whether the Buendia API server is up and responding to HTTP
 * requests at the URL in the "OpenMRS root URL" preference setting.
 */
public class BuendiaApiHealthCheck extends HealthCheck {

    private static final Logger LOG = Logger.create();

    // Under normal conditions, make requests to the server with this frequency
    // to check if it's reachable and responding.
    private static final int CHECK_PERIOD_MS = 10000;

    // During certain problem conditions, check more often so that when the
    // problem is resolved, we can hide the snackbar more promptly.
    private static final int FAST_CHECK_PERIOD_MS = 1000;

    // These are the issues for which we use the faster checking period.
    private static final Set<HealthIssue> FAST_CHECK_ISSUES = ImmutableSet.of(
        HealthIssue.SERVER_HOST_UNREACHABLE,
        HealthIssue.SERVER_NOT_RESPONDING
    );

    // Retrieving a concept should be quick and ensures that the module is
    // running and has database access.
    private static final String HEALTH_CHECK_ENDPOINT =
        "/concepts/" + ConceptUuids.PULSE_UUID;

    private final Object mLock = new Object();

    private final OpenMrsConnectionDetails mConnectionDetails;

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private BuendiaModuleHealthCheckRunnable mRunnable;

    BuendiaApiHealthCheck(
        Application application,
        OpenMrsConnectionDetails connectionDetails) {
        super(application);

        mConnectionDetails = connectionDetails;
    }

    @Override protected void startImpl() {
        synchronized (mLock) {
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread("Buendia API Health Check");
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper());
            }

            if (mRunnable == null) {
                mRunnable = new BuendiaModuleHealthCheckRunnable(mHandler);
            }

            if (!mRunnable.isRunning.getAndSet(true)) {
                mHandler.post(mRunnable);
            }
        }
    }

    @Override protected void stopImpl() {
        synchronized (mLock) {
            if (mRunnable != null) {
                mRunnable.isRunning.set(false);
                mRunnable = null;
            }

            if (mHandlerThread != null) {
                mHandlerThread.quit();
                mHandlerThread = null;
            }

            mHandler = null;
        }
    }

    protected int getCheckPeriodMillis() {
        return Sets.intersection(FAST_CHECK_ISSUES, mActiveIssues).isEmpty()
            ? CHECK_PERIOD_MS : FAST_CHECK_PERIOD_MS;
    }

    private class BuendiaModuleHealthCheckRunnable implements Runnable {
        public final AtomicBoolean isRunning;

        private final Handler mHandler;

        public BuendiaModuleHealthCheckRunnable(Handler handler) {
            isRunning = new AtomicBoolean(false);
            mHandler = handler;
        }

        @Override public void run() {
            if (!isRunning.get()) return;

            try {
                Uri uri = Uri.parse(mConnectionDetails.getBuendiaApiUrl() + HEALTH_CHECK_ENDPOINT);
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(uri.toString());
                if (httpGet.getURI().getHost() == null) {
                    LOG.w("Configured OpenMRS server URL is invalid: %s", uri);
                    reportIssue(HealthIssue.SERVER_CONFIGURATION_INVALID);
                    return;
                }

                try {
                    httpGet.addHeader(BasicScheme.authenticate(
                        new UsernamePasswordCredentials(
                            mConnectionDetails.getUser(),
                            mConnectionDetails.getPassword()),
                        "UTF-8", false));
                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    if (httpResponse.getStatusLine().getStatusCode() != HttpURLConnection.HTTP_OK) {
                        LOG.w("The OpenMRS URL '%1$s' returned unexpected error code: %2$s",
                            uri, httpResponse.getStatusLine().getStatusCode());
                        switch (httpResponse.getStatusLine().getStatusCode()) {
                            // TODO(ping): Detect whether the HTTP error code is coming
                            // from a Buendia server or a zealous network firewall.
                            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                                reportIssue(HealthIssue.SERVER_INTERNAL_ISSUE);
                                break;
                            case HttpURLConnection.HTTP_FORBIDDEN:
                            case HttpURLConnection.HTTP_UNAUTHORIZED:
                                reportIssue(HealthIssue.SERVER_AUTHENTICATION_ISSUE);
                                break;
                            case HttpURLConnection.HTTP_NOT_FOUND:
                            default:
                                reportIssue(HealthIssue.SERVER_NOT_RESPONDING);
                                break;
                        }
                        // TODO(ping): Confirm that the content returned is valid JSON,
                        // as some firewalls can return 200 with an error page.
                        if (hasIssue(HealthIssue.SERVER_HOST_UNREACHABLE)){
                            resolveIssue(HealthIssue.SERVER_HOST_UNREACHABLE);
                        }
                        return;
                    }
                } catch (UnknownHostException | IllegalArgumentException e) {
                    LOG.w("OpenMRS server unreachable: %s", uri);
                    reportIssue(HealthIssue.SERVER_HOST_UNREACHABLE);
                    return;
                } catch (HttpHostConnectException e) {
                    LOG.w("OpenMRS server connection refused: %s", e.getHost());
                } catch (IOException e) {
                    LOG.w("OpenMRS server health check failed: %s", uri);
                    return;
                }

                resolveAllIssues();
            } finally {
                mHandler.postDelayed(this, getCheckPeriodMillis());
            }
        }
    }
}
