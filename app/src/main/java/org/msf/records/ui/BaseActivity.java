package org.msf.records.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import org.msf.records.App;

import de.greenrobot.event.EventBus;

/**
 * An abstract {@link FragmentActivity} that is the base for all activities.
 */
public abstract class BaseActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.getInstance().inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
    }
}

