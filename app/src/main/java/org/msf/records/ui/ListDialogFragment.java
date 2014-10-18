package org.msf.records.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.msf.records.R;
import org.msf.records.model.ListItem;

import java.util.Arrays;

/**
 * Created by danieljulio on 06/10/2014.
 */

public class ListDialogFragment extends DialogFragment {

    private static final String TAG = ListDialogFragment.class.getName();
    private static final String ITEM_LIST_KEY = "ITEM_LIST_KEY";
    public static final int TYPE_PRIMARY = 0, TYPE_SECONDARY = 1;

    ArrayAdapterWithIcon mListAdapter;
    ListView mListView;
    ListItem mData;
    Parcelable[] mParcelables;
    ListItem[] mIconListDialogs;
    OnItemClickListener mItemClickListener;

    interface OnItemClickListener {
        public void onListItemClick(int position, int type);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mListAdapter = new ArrayAdapterWithIcon(getActivity());
        Bundle bundle = getArguments();
        mParcelables = bundle.getParcelableArray(ITEM_LIST_KEY);
        mIconListDialogs = Arrays.copyOf(mParcelables, mParcelables.length, ListItem[].class);

        mListAdapter.addAll(mIconListDialogs);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(getParentFragment() instanceof OnItemClickListener)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mItemClickListener = (OnItemClickListener) getParentFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mListView = new ListView(getActivity());

        mListView.setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mItemClickListener.onListItemClick(position, TYPE_PRIMARY);
                getDialog().dismiss();
            }
        });

        return mListView;
    }


    public class ArrayAdapterWithIcon extends ArrayAdapter<ListItem> {

        public ArrayAdapterWithIcon(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_cell_dialog_row_with_icons, null);

            mData = getItem(position);
            ImageView image = (ImageView) convertView.findViewById(R.id.listview_cell_dialog_icon);

            Log.d(TAG, "subLocation: " + mData.getFurtherDialogId());

            if (mData.getObjectId() == 0) {
                getDialog().setTitle("Status");
                image.setImageResource(mData.getIconId());
            }

            else if (mData.getObjectId() == 1) {
                getDialog().setTitle("Location");
                image.setVisibility(View.GONE);
            }

            TextView title = (TextView) convertView.findViewById(R.id.listview_cell_dialog_title);
            title.setText(mData.getTitleId());
            return convertView;
        }
    }
}