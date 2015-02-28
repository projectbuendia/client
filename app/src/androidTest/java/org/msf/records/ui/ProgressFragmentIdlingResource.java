package org.msf.records.ui;

import com.google.android.apps.common.testing.ui.espresso.IdlingResource;

/**
 * An {@link IdlingResource} that waits for a ProgressFragment to be ready before continuing.
 */
public class ProgressFragmentIdlingResource implements IdlingResource {
    private String mName;
    private ProgressFragment mProgressFragment;
    private ResourceCallback mResourceCallback;

    /**
     * Constructs a new idling resource that will wait on the given {@link ProgressFragment} to
     * not be in the LOADED state before continuing. Resources with the same name as an
     * existing resource will be ignored.
     * @param name a unique name for idempotency
     * @param progressFragment the {@link ProgressFragment} to monitor
     */
    public ProgressFragmentIdlingResource(String name, ProgressFragment progressFragment) {
        mName = name;
        mProgressFragment = progressFragment;
        mProgressFragment.registerSubscriber(new ProgressFragmentIdleSubscriber());
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public boolean isIdleNow() {
        return mProgressFragment.getState() == ProgressFragment.State.LOADED;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }

    private class ProgressFragmentIdleSubscriber implements ProgressFragment.ChangeStateSubscriber {

        @Override
        public void onChangeState(ProgressFragment.State newState) {
            if (mResourceCallback != null && isIdleNow()) {
                mResourceCallback.onTransitionToIdle();
                mProgressFragment.unregisterSubscriber(this);
            }
        }
    }
}
