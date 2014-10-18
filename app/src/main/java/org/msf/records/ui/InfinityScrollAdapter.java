package org.msf.records.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gil on 13/10/2014.
 */
public class InfinityScrollAdapter<T>  {


    /*private List<T> items  = new ArrayList<T>();

    private Context context;


    public ResultsAdapter(Context context) {
        context = context;
        loadNextPage();
    }

    public void setQuery(String query) {
        mQuery = query;
        if (mInFlightRequest != null)
            mInFlightRequest.cancel();
        mInFlightRequest = null;
        mPhotos.clear();
        notifyDataSetChanged();
        loadNextPage();
    }

    private void loadNextPage() {
        if (mInFlightRequest != null) {
            return;
        }

        int page = (int) (mPhotos.size() / (double) PER_PAGE);
        Toast.makeText(mContext, "Loading page: " + (page + 1), Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public T getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }*/
}
