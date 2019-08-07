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

package org.projectbuendia.client.ui;

import android.support.test.espresso.IdlingResource;

/** An {@link IdlingResource} that waits for a ProgressFragment to be ready before continuing. */
public class ProgressFragmentIdlingResource implements IdlingResource {
    private String mName;
    private ProgressFragment mProgressFragment;
    private ResourceCallback mResourceCallback;

    /**
     * Constructs a new idling resource that will wait on the given {@link ProgressFragment} to
     * not be in the READY state before continuing. Resources with the same name as an
     * existing resource will be ignored.
     * @param name             a unique name for idempotency
     * @param progressFragment the {@link ProgressFragment} to monitor
     */
    public ProgressFragmentIdlingResource(String name, ProgressFragment progressFragment) {
        mName = name;
        mProgressFragment = progressFragment;
        mProgressFragment.registerSubscriber(new ProgressFragmentIdleSubscriber());
    }

    @Override public String getName() {
        return mName;
    }

    @Override public boolean isIdleNow() {
        return mProgressFragment.getState() == ReadyState.READY;
    }

    @Override public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }

    private class ProgressFragmentIdleSubscriber implements ProgressFragment.ReadyStateSubscriber {

        @Override public void onChangeState(ReadyState newState) {
            if (mResourceCallback != null && isIdleNow()) {
                mResourceCallback.onTransitionToIdle();
                mProgressFragment.unregisterSubscriber(this);
            }
        }
    }
}
