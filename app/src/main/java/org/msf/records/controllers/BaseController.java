package org.msf.records.controllers;

import android.content.Intent;

import org.msf.records.ui.ControllableActivity;
import org.msf.records.ui.ControllableFragment;

/**
 * A convenient abstract controller that can control both {@link ControllableActivity} and
 * {@link android.app.Fragment} instances.
 */
public abstract class BaseController implements ActivityController, FragmentController {

    public void register(ControllableActivity activity) {
        activity.registerController(this);
    }

    public void register(ControllableFragment fragment) {
        fragment.registerController(this);
    }

    public void unregister(ControllableActivity activity) {
        activity.unregisterController(this);
    }

    public void unregister(ControllableFragment fragment) {
        fragment.unregisterController(this);
    }

    @Override
    public void onActivityResult(
            ControllableActivity activity, int requestCode, int resultCode, Intent data) {}

    @Override
    public void onFragmentResume() {}
}
