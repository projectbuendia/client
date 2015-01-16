package org.msf.records.diagnostics;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import org.msf.records.prefs.StringPreference;
import org.msf.records.utils.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link HealthCheck} that checks the status of an HTTP server.
 */
public class HttpServerHealthCheck extends HealthCheck {

    private static final Logger LOG = Logger.create();

    private static final int CHECK_FREQUENCY_MS = 20000;
    private static final int SERVER_REACHABILITY_TIMEOUT_MS = 2000;

    private final Object mLock = new Object();

    private final StringPreference mOpenMrsRootUrl;

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private HttpServerHealthCheckRunnable mRunnable;

    public HttpServerHealthCheck(
            Application application,
            StringPreference openMrsRootUrl) {
        super(application);

        mOpenMrsRootUrl = openMrsRootUrl;
    }

    @Override
    protected void startImpl() {
        synchronized (mLock) {
            if (mHandlerThread == null) {
                mHandlerThread = new HandlerThread("HTTP Server Health Check");
                mHandlerThread.start();
                mHandler = new Handler(mHandlerThread.getLooper());
            }

            if (mRunnable == null) {
                mRunnable = new HttpServerHealthCheckRunnable(mHandler);
            }

            if (!mRunnable.isRunning.getAndSet(true)) {
                mHandler.post(mRunnable);
            }
        }
    }

    @Override
    protected void stopImpl() {
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

    private class HttpServerHealthCheckRunnable implements Runnable {

        public final AtomicBoolean isRunning;

        private final Handler mHandler;

        public HttpServerHealthCheckRunnable(Handler handler) {
            isRunning = new AtomicBoolean(false);
            mHandler = handler;
        }

        @Override
        public void run() {
            if (!isRunning.get()) {
                return;
            }

            try {
                String uriString = mOpenMrsRootUrl.get();
                Uri uri = Uri.parse(uriString);
                if (uri.equals(Uri.EMPTY)) {
                    LOG.w("The configured OpenMRS root URL '%1$s' is invalid.", uriString);
                    reportIssue(HealthIssue.SERVER_CONFIGURATION_INVALID);
                    return;
                }

                InetAddress address;
                try {
                    address = InetAddress.getByName(uri.getHost());
                } catch (UnknownHostException e) {
                    LOG.w(
                            "The configured OpenMRS root URL '%1$s' has an unknown host.",
                            uriString);
                    reportIssue(HealthIssue.SERVER_CONFIGURATION_INVALID);
                    return;
                }

                try {
                    if (!address.isReachable(SERVER_REACHABILITY_TIMEOUT_MS)) {
                        LOG.w(
                                "The host of the configured OpenMRS root URL '%1$s' could not be "
                                        + "reached",
                                uriString);
                        reportIssue(HealthIssue.SERVER_HOST_UNREACHABLE);
                        return;
                    }
                } catch (IOException e) {
                    LOG.w(
                            "The host of the configured OpenMRS root URL '%1$s' could be "
                                    + "reached.",
                            uriString);
                    reportIssue(HealthIssue.SERVER_HOST_UNREACHABLE);
                    return;
                }

               resolveAllIssues();
            } finally {
                mHandler.postDelayed(this, CHECK_FREQUENCY_MS);
            }
        }
    }
}
