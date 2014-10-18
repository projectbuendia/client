package org.msf.records.ui;



import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.msf.records.R;
import org.msf.records.model.BloodTest;

import java.util.ArrayList;
import java.util.TreeSet;

import butterknife.ButterKnife;
import butterknife.InjectView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PatientDetailBloodFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class PatientDetailBloodFragment extends ProgressFragment {


    private String mPatientId;

    private BloodTestsAdapter mAdapter;

    @InjectView(R.id.patient_blood_list) ListView mListview;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param patientId The patient id.
     * @return A new instance of fragment PatientDetailFlagsFragment.
     */
    public static PatientDetailBloodFragment newInstance(String patientId) {
        PatientDetailBloodFragment fragment = new PatientDetailBloodFragment();
        Bundle args = new Bundle();
        args.putString(PatientDetailFragment.PATIENT_ID_KEY, patientId);
        fragment.setArguments(args);
        return fragment;
    }

    public PatientDetailBloodFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPatientId = getArguments().getString(PatientDetailFragment.PATIENT_ID_KEY);
        }
        setContentView(R.layout.fragment_patient_detail_blood);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.inject(this, view);
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new BloodTestsAdapter(getActivity());
        if (mListview.getHeaderViewsCount() == 0)//hack to stop multiple headers
            mListview.addHeaderView(LayoutInflater.from(getActivity()).inflate(R.layout.include_patient_detail_blood_tests_header, null), null, false);
        mAdapter.addSectionHeaderItem("Open lab tests");
        for (BloodTest bloodTest : BloodTest.GETDUMMYDATA1())
            mAdapter.addItem(bloodTest);
        //mAdapter.addSectionHeaderItem("Completed lab tests");
        //for (Flag flag : Flag.GETDUMMYDATA2())
        //    mAdapter.addItem(flag);
        mListview.setAdapter(mAdapter);
        changeState(State.LOADED);
    }


    class BloodTestsAdapter extends BaseAdapter {

        private static final int TYPE_ITEM_ON_GOING = 0;
        private static final int TYPE_ITEM_PAST_BLOOD_TEST = 2;
        private static final int TYPE_SEPARATOR = 1;

        private ArrayList mData = new ArrayList();
        private TreeSet<Integer> sectionHeader = new TreeSet<Integer>();

        private LayoutInflater mInflater;

        public BloodTestsAdapter(Context context) {
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(final BloodTest item) {
            mData.add(item);
            notifyDataSetChanged();
        }

        public void addSectionHeaderItem(final String item) {
            mData.add(item);
            sectionHeader.add(mData.size() - 1);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (sectionHeader.contains(position)) {
                return TYPE_SEPARATOR;
            }
            BloodTest flag = ((BloodTest) getItem(position));
            if (flag.completed != null)
                return TYPE_ITEM_PAST_BLOOD_TEST;
            return TYPE_ITEM_ON_GOING;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            int itemType = getItemViewType(position);
            switch (itemType) {
                case TYPE_ITEM_ON_GOING:
                    BloodTest bloodTest = (BloodTest) getItem(position);
                    if (convertView == null || (Integer) convertView.getTag() != itemType) {
                        convertView = mInflater.inflate(R.layout.listview_cell_patient_blood_tests, null);
                        convertView.setTag(itemType);
                    }

                    TextView status = (TextView) convertView.findViewById(R.id.patient_detail_blood_tests_list_row_status_type);
                    status.setText(bloodTest.type);
                    TextView contents = (TextView) convertView.findViewById(R.id.patient_detail_blood_tests_list_row_contents);
                    contents.setText(bloodTest.comment);
                    TextView doneBtn = (TextView) convertView.findViewById(R.id.patient_detail_blood_tests_list_row_done_btn);
                    doneBtn.setVisibility(View.VISIBLE);
                    break;
                case TYPE_SEPARATOR:
                    if (convertView == null || (Integer) convertView.getTag() != itemType) {
                        convertView = mInflater.inflate(R.layout.include_listview_header, null);
                        convertView.setTag(itemType);
                    }

                    TextView title = (TextView) convertView.findViewById(R.id.patient_listview_header_title);
                    title.setText((String) getItem(position));
                    break;
                case TYPE_ITEM_PAST_BLOOD_TEST:
                    BloodTest pastFlag = (BloodTest) getItem(position);
                    if (convertView == null || (Integer) convertView.getTag() != itemType) {
                        convertView = mInflater.inflate(R.layout.listview_cell_patient_flags, null);
                        convertView.setTag(itemType);
                    }

                    TextView pastStatus = (TextView) convertView.findViewById(R.id.patient_detail_blood_tests_list_row_status_type);
                    pastStatus.setText(pastFlag.type);
                    TextView pastContents = (TextView) convertView.findViewById(R.id.patient_detail_blood_tests_list_row_contents);
                    pastContents.setText("Completed x ago");
                    TextView pastDoneBtn = (TextView) convertView.findViewById(R.id.patient_detail_blood_tests_list_row_done_btn);
                    pastDoneBtn.setVisibility(View.GONE);
                    break;
            }
            return convertView;
        }


    }
}