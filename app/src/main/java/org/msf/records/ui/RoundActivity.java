package org.msf.records.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

import org.msf.records.R;
import org.msf.records.utils.PatientCountDisplay;

public class RoundActivity extends PatientSearchActivity {
    private String mTentName;
    private String mTentUuid;

    private int mTentPatientCount;

    public static final String TENT_NAME_KEY = "tent_name";
    public static final String TENT_PATIENT_COUNT_KEY = "tent_patient_count";
    public static final String TENT_UUID_KEY = "tent_uuid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mTentName = getIntent().getStringExtra(TENT_NAME_KEY);
            mTentPatientCount = getIntent().getIntExtra(TENT_PATIENT_COUNT_KEY, 0);
            mTentUuid = getIntent().getStringExtra(TENT_UUID_KEY);
        } else {
            mTentName = savedInstanceState.getString(TENT_NAME_KEY);
            mTentPatientCount = getIntent().getIntExtra(TENT_PATIENT_COUNT_KEY, 0);
            mTentUuid = savedInstanceState.getString(TENT_UUID_KEY);
        }

        setTitle(PatientCountDisplay.getPatientCountTitle(this, mTentPatientCount, mTentName));
        setContentView(R.layout.activity_round);

        // TODO(akalachman): Initialize fragment.
    }

    @Override
    public void onExtendOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        // TODO(akalachman): Activate search / fragment selection.
    }
}
