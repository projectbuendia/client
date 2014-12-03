package org.msf.records.ui;

import android.support.v4.app.Fragment;
import android.os.Bundle;

import org.msf.records.controllers.ActivityController;
import org.msf.records.controllers.FragmentController;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Fragment} that allows {@link FragmentController}s to register themselves.
 */
public class ControllableFragment extends Fragment {

    private List<FragmentController> mControllers;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mControllers = new ArrayList<FragmentController>();
    }

    public void registerController(FragmentController controller) {
        mControllers.add(controller);
    }

    public void unregisterController(FragmentController controller) {
        mControllers.remove(controller);
    }
}
