package org.msf.records.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.Serializable;

/**
 * Created by danieljulio on 06/10/2014.
 */
@Deprecated
public class ListDialogFragment extends DialogFragment {

    private static final String TAG = ListDialogFragment.class.getName();
    private static final String ITEM_LIST_KEY = "ITEM_LIST_KEY";
    public static final String GRID_ITEM_DONE_LISTENER = "GRID_ITEM_DONE_LISTENER";

    ArrayAdapter<String> mListAdapter;
    ListView mListView;
    OnItemClickListener mItemClickListener;
    String[] mArray;

    public interface OnItemClickListener extends Serializable {
        void onListItemClick(int position);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Bundle bundle = getArguments();
        mArray = bundle.getStringArray(ITEM_LIST_KEY);
        mListAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, mArray);
        mItemClickListener = (OnItemClickListener) bundle.getSerializable(GRID_ITEM_DONE_LISTENER);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        getDialog().setTitle("Edit Patient Info");
        mListView = new ListView(getActivity());

        mListView.setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mItemClickListener.onListItemClick(position);
                getDialog().dismiss();
            }
        });

        return mListView;
    }

}