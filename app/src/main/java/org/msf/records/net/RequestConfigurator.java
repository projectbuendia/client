package org.msf.records.net;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;

/**
 * An object that configures Volley requests before they are sent to the server.
 *
 * <p>The default implementation of this class sets the retry policy to a configurable timeout and
 * to not have any retries.
 */
class RequestConfigurator {

    private final RetryPolicy mRetryPolicy;

    RequestConfigurator(int timeoutMs) {
        mRetryPolicy =
                new DefaultRetryPolicy(timeoutMs, 0 /*maxNumRetries*/, 0 /*backoffMultiplier*/);
    }

    public <T extends Request> T configure(T request) {
        request.setRetryPolicy(mRetryPolicy);
        return request;
    }
}
