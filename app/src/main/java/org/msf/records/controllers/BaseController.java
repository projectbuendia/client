package org.msf.records.controllers;

import android.content.Intent;

import org.msf.records.ui.ControllableActivity;

/**
 * A convenient abstract controller that can control both {@link ControllableActivity} and
 * {@link android.app.Fragment} instances.
 */
public abstract class BaseController implements ActivityController {

    public void register(ControllableActivity activity) {
        activity.registerController(this);
    }


    public void unregister(ControllableActivity activity) {
        activity.unregisterController(this);
    }


    @Override
    public void onActivityResult(
            ControllableActivity activity, int requestCode, int resultCode, Intent data) {}
}
