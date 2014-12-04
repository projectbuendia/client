package org.msf.records.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;

import org.msf.records.controllers.ActivityController;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link FragmentActivity} that allows {@link ActivityController}s to register themselves.
 */
public abstract class ControllableActivity extends FragmentActivity {

    private List<ActivityController> mControllers;

    public void registerController(ActivityController controller) {
        mControllers.add(controller);
    }

    public void unregisterController(ActivityController controller) {
        mControllers.remove(controller);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mControllers = new ArrayList<ActivityController>();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        for (ActivityController controller : mControllers) {
            controller.onActivityResult(this, requestCode, resultCode, data);
        }
    }
}
