package org.msf.records.ui;



import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.msf.records.R;
import org.msf.records.model.Flag;

import java.util.ArrayList;
import java.util.TreeSet;

import butterknife.ButterKnife;
import butterknife.InjectView;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PatientDetailFlagsFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class PatientDetailFlagsFragment extends ProgressFragment {


    private String mPatientId;

    private FlagsAdapter mAdapter;

    @InjectView(R.id.patient_flags_list) ListView mListview;



    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param patientId The patient id.
     * @return A new instance of fragment PatientDetailFlagsFragment.
     */
    public static PatientDetailFlagsFragment newInstance(String patientId) {
        PatientDetailFlagsFragment fragment = new PatientDetailFlagsFragment();
        Bundle args = new Bundle();
        args.putString(PatientDetailFragment.PATIENT_ID_KEY, patientId);
        fragment.setArguments(args);
        return fragment;
    }
    public PatientDetailFlagsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPatientId = getArguments().getString(PatientDetailFragment.PATIENT_ID_KEY);
        }
        setContentView(R.layout.fragment_patient_detail_flags);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.inject(this, view);
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new FlagsAdapter(getActivity());
        if(mListview.getHeaderViewsCount() == 0)//hack to stop multiple headers
            mListview.addHeaderView(LayoutInflater.from(getActivity()).inflate(R.layout.include_patient_detail_flag_header, null), null, false);
        mAdapter.addSectionHeaderItem("Current flags");
        for(Flag flag : Flag.GETDUMMYDATA1())
            mAdapter.addItem(flag);
        mAdapter.addSectionHeaderItem("Past flags");
        for(Flag flag : Flag.GETDUMMYDATA2())
            mAdapter.addItem(flag);
        mListview.setAdapter(mAdapter);
        changeState(State.LOADED);
    }

    class FlagsAdapter extends BaseAdapter {

        private static final int TYPE_ITEM_ON_GOING = 0;
        private static final int TYPE_ITEM_PAST_FLAG = 2;
        private static final int TYPE_SEPARATOR = 1;

        private ArrayList mData = new ArrayList();
        private TreeSet<Integer> sectionHeader = new TreeSet<Integer>();

        private LayoutInflater mInflater;

        public FlagsAdapter(Context context) {
            mInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(final Flag item) {
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
            if(sectionHeader.contains(position)){
                return TYPE_SEPARATOR;
            }
            Flag flag = ((Flag) getItem(position));
            if(flag.completed != null)
                return TYPE_ITEM_PAST_FLAG;
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
                    Flag flag = (Flag) getItem(position);
                    if(convertView == null || (Integer) convertView.getTag() != itemType) {
                        convertView = mInflater.inflate(R.layout.listview_cell_patient_flags, null);
                        convertView.setTag(itemType);
                    }

                    TextView status = (TextView) convertView.findViewById(R.id.patient_detail_flag_list_row_status_type);
                    status.setText(flag.type);
                    TextView contents = (TextView) convertView.findViewById(R.id.patient_detail_flag_list_row_contents);
                    contents.setText(flag.comment);
                    TextView viewBtn = (TextView) convertView.findViewById(R.id.patient_detail_flag_list_row_view_btn);
                    viewBtn.setVisibility(View.GONE);
                    TextView undoBtn = (TextView) convertView.findViewById(R.id.patient_detail_flag_list_row_undo_btn);
                    undoBtn.setVisibility(View.GONE);
                    TextView doneBtn = (TextView) convertView.findViewById(R.id.patient_detail_flag_list_row_done_btn);
                    doneBtn.setVisibility(View.VISIBLE);
                    break;
                case TYPE_SEPARATOR:
                    if(convertView == null || (Integer) convertView.getTag() != itemType) {
                        convertView = mInflater.inflate(R.layout.include_listview_header, null);
                        convertView.setTag(itemType);
                    }

                    TextView title = (TextView) convertView.findViewById(R.id.patient_listview_header_title);
                    title.setText((String) getItem(position));
                    break;
                case TYPE_ITEM_PAST_FLAG:
                    Flag pastFlag = (Flag) getItem(position);
                    if(convertView == null || (Integer) convertView.getTag() != itemType) {
                        convertView = mInflater.inflate(R.layout.listview_cell_patient_flags, null);
                        convertView.setTag(itemType);
                    }

                    TextView pastStatus = (TextView) convertView.findViewById(R.id.patient_detail_flag_list_row_status_type);
                    pastStatus.setText(pastFlag.type);
                    TextView pastContents = (TextView) convertView.findViewById(R.id.patient_detail_flag_list_row_contents);
                    pastContents.setText("Completed x ago");
                    TextView pastViewBtn = (TextView) convertView.findViewById(R.id.patient_detail_flag_list_row_view_btn);
                    pastViewBtn.setVisibility(View.VISIBLE);
                    TextView pastUndoBtn = (TextView) convertView.findViewById(R.id.patient_detail_flag_list_row_undo_btn);
                    pastUndoBtn.setVisibility(View.VISIBLE);
                    TextView pastDoneBtn = (TextView) convertView.findViewById(R.id.patient_detail_flag_list_row_done_btn);
                    pastDoneBtn.setVisibility(View.GONE);
                    break;
            }
            return convertView;
        }

    }


}
