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

package org.msf.records.net;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;

/**
 * Configures Volley requests before they are sent to the server.
 *
 * <p>The default implementation of this class sets the retry policy to a configurable timeout and
 * no retries.
 */
class RequestConfigurator {

    private final RetryPolicy mRetryPolicy;

    RequestConfigurator(int timeoutMs) {
        mRetryPolicy =
                new DefaultRetryPolicy(timeoutMs, 0 /*maxNumRetries*/, 0 /*backoffMultiplier*/);
    }

    RequestConfigurator(int timeoutMs, int maxNumRetries, int backoffMultiplier) {
        mRetryPolicy = new DefaultRetryPolicy(timeoutMs, maxNumRetries, backoffMultiplier);
    }

    public <T extends Request> T configure(T request) {
        request.setRetryPolicy(mRetryPolicy);
        return request;
    }
}
