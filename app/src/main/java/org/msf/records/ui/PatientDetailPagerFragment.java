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
 * Created by akalachman on 11/19/14.
 */
public class PatientDetailPagerFragment extends ProgressFragment {
    private static final int COUNT = 2;
    private static final int OVERVIEW = 0, VIEW_CHART = 1;//FLAGS = 1, BLOOD = 2, LOGS = 3;

    public static final String PATIENT_ID_KEY = "PATIENT_ID_KEY";

    public String mPatientId;

    ViewPager mPager;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_patient_detail_pager, container, false);

        FragmentPagerAdapter adapter = new PatientDetailsAdapter(getChildFragmentManager());

        mPager = (ViewPager) view.findViewById(R.id.pager);
        mPager.setAdapter(adapter);

        TabPageIndicator indicator = (TabPageIndicator) view.findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
        indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {}

            @Override
            public void onPageSelected(int i) {
                mPager.requestLayout();
            }

            @Override
            public void onPageScrollStateChanged(int i) {}
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = savedInstanceState != null ? savedInstanceState : getArguments();
        mPatientId = bundle.getString(PATIENT_ID_KEY);
        if(mPatientId == null)
            throw new IllegalArgumentException(
                    "Please pass the user id to the PatientDetailPagerFragment");
        // setContentView(R.layout.fragment_patient_detail_pager);
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
                /*case BLOOD:
                    return PatientDetailBloodFragment.newInstance(mPatientId);*/
                case VIEW_CHART:
                    return PatientDetailViewChartFragment.newInstance(mPatientId);
                /*case FLAGS:
                    return PatientDetailFlagsFragment.newInstance(mPatientId);*/
                default:
                    return null;

            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case OVERVIEW:
                    return getString(R.string.detail_pager_overview);
                case VIEW_CHART:
                    return getString(R.string.detail_page_view_chart);
                /*case BLOOD:
                    return getString(R.string.detail_pager_blood);
                case LOGS:
                    return getString(R.string.detail_pager_logs);
                case FLAGS:
                    return getString(R.string.detail_pager_flags);*/
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
