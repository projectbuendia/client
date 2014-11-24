package org.msf.records.ui;

import android.app.ActionBar;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;

import org.msf.records.App;
import org.msf.records.R;

import de.greenrobot.event.EventBus;

/**
 * An abstract {@link FragmentActivity} that is the base for all activities.
 */
public abstract class BaseActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        ActionBar actionBar = getActionBar();
//        actionBar.setCustomView(R.layout.)
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

    /**
     * Prevents the menu from being created. Subclasses cannot override.
     */
    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    public boolean onExtendOptionsMenu(Menu menu) {
        return false;
    }
}
