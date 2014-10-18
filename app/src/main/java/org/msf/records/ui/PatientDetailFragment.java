package org.msf.records.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.viewpagerindicator.TabPageIndicator;

import org.msf.records.R;

/**
 * A fragment representing a single Patient detail screen.
 * This fragment is either contained in a {@link PatientListActivity}
 * in two-pane mode (on tablets) or a {@link PatientDetailActivity}
 * on handsets.
 */
public class PatientDetailFragment extends Fragment {

    private static final String TAG = PatientDetailFragment.class.getSimpleName();

    public static final String PATIENT_ID_KEY = "PATIENT_ID_KEY";

    private static final int COUNT = 4;
    private static final int OVERVIEW = 0, FLAGS = 1, BLOOD = 2, LOGS = 3;

    public String mPatientId;

    ViewPager mPager;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_detail, container, false);

        FragmentPagerAdapter adapter = new PatientDetailsAdapter(getChildFragmentManager());

        mPager = (ViewPager) view.findViewById(R.id.pager);
        mPager.setAdapter(adapter);

        TabPageIndicator indicator = (TabPageIndicator) view.findViewById(R.id.indicator);
        indicator.setViewPager(mPager);


        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = savedInstanceState != null ? savedInstanceState : getArguments();
        mPatientId = bundle.getString(PATIENT_ID_KEY);
        if(mPatientId == null)
            throw new IllegalArgumentException("Please pass the user id to the PatientDetailFragment");

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PATIENT_ID_KEY, mPatientId);
    }

    class PatientDetailsAdapter extends FragmentPagerAdapter {
        public PatientDetailsAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case OVERVIEW:
                    return PatientDetailOverviewFragment.newInstance(mPatientId);
                case BLOOD:
                    return PatientDetailBloodFragment.newInstance(mPatientId);
                case LOGS:
                    return PatientDetailLogsFragment.newInstance();
                case FLAGS:
                    return PatientDetailFlagsFragment.newInstance(mPatientId);
                default:
                    return null;

            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case OVERVIEW:
                    return getString(R.string.detail_pager_overview);
                case BLOOD:
                    return getString(R.string.detail_pager_blood);
                case LOGS:
                    return getString(R.string.detail_pager_logs);
                case FLAGS:
                    return getString(R.string.detail_pager_flags);
                default:
                    return null;

            }
        }

        @Override
        public int getCount() {
            return COUNT;
        }
    }
}