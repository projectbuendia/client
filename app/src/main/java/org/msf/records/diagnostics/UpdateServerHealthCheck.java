package org.msf.records.diagnostics;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.msf.records.prefs.StringPreference;
import org.msf.records.utils.Logger;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * A {@link HealthCheck} that checks whether the update server is up and running.
 */
public class UpdateServerHealthCheck extends HealthCheck {

    private static final Logger LOG = Logger.create();

    /** Check for issues with this frequency. */
    private static final int CHECK_PERIOD_MS = 20000;

    private static final String HEALTH_CHECK_ENDPOINT = "/dists/stable/Release";

    private final Object mLock = new Object();

    private final StringPreference mRootUrl;

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    UpdateServerHealthCheck(Application application, StringPreference rootUrl) {
        super(application);
        mRootUrl = rootUrl;
    }

    @Override
    protected void startImpl() {
        synchronized (mLock) {
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread("Buendia Update Server Health Check");
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

    private final Runnable mHealthCheckRunnable = new Runnable() {

        private void performCheck() {
            Uri uri = Uri.parse(mRootUrl.get() + HEALTH_CHECK_ENDPOINT);

            HttpClient httpClient = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet(uri.toString());
            try {
                HttpResponse response = httpClient.execute(getRequest);
                switch (response.getStatusLine().getStatusCode()) {
                    case HttpStatus.SC_OK:
                        LOG.d("Update server check completed, OK.");
                        resolveAllIssues();
                        return;
                    case HttpStatus.SC_NOT_FOUND:
                        LOG.d("Update server check completed, 404.");
                        // The update server is reachable if we get a 404.
                        resolveIssue(HealthIssue.UPDATE_SERVER_HOST_UNREACHABLE);
                        reportIssue(HealthIssue.UPDATE_SERVER_INDEX_NOT_FOUND);
                        return;
                    default:
                        LOG.w("Update server check failed for URI %1$s.", uri);
                }
            } catch (UnknownHostException e) {
                LOG.d("Update server unreachable");
                reportIssue(HealthIssue.UPDATE_SERVER_HOST_UNREACHABLE);
            } catch (IOException e) {
                LOG.w(e, "Update server check failed for URI %1$s.", uri);
            }
        }

        @Override
        public void run() {
            performCheck();

            synchronized (mLock) {
                mHandler.postDelayed(this, CHECK_PERIOD_MS);
            }
        }
    };
}
