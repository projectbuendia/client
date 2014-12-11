package org.msf.records.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * SectionSpinnerAdapter is an ArrayAdapter that uses null values to
 * indicate 'section' boundaries. Section boundaries are represented by
 * an arbitrary resource and are not clickable.
 */
public class SectionedSpinnerAdapter<T> extends ArrayAdapter<T> {
    private final T[] mItems;
    private final int mSectionBorderResource;
    private final LayoutInflater mInflater;

    private enum ViewType {
        SECTION_BORDER, LIST_ITEM
    }

    public SectionedSpinnerAdapter(
            Context context, int collapsedResource, int dropDownResource,
            int sectionBorderResource, T[] items) {
        super(context, collapsedResource, items);
        mItems = items;
        mSectionBorderResource = sectionBorderResource;
        setDropDownViewResource(dropDownResource);

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (isSectionBorder(position)) {
            return getSectionBorder(convertView, parent);
        }

        resetClickable(convertView);
        if (convertView != null && convertView.getTag() != ViewType.LIST_ITEM) {
            convertView = null;
        }

        View view = super.getDropDownView(position, convertView, parent);
        // Manually manage the different types of views, since Spinners don't
        // ordinarily support multiple view types and ignore getViewTypeCount().
        view.setTag(ViewType.LIST_ITEM);
        return view;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (isSectionBorder(position)) {
            return getSectionBorder(convertView, parent);
        }

        resetClickable(convertView);
        if (convertView != null && convertView.getTag() != ViewType.LIST_ITEM) {
            convertView = null;
        }

        View view = super.getView(position, convertView, parent);
        // Manually manage the different types of views, since Spinners don't
        // ordinarily support multiple view types and ignore getViewTypeCount().
        view.setTag(ViewType.LIST_ITEM);
        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public int getItemViewType(int position) {
        return isSectionBorder(position) ?
                ViewType.SECTION_BORDER.ordinal() :
                ViewType.LIST_ITEM.ordinal();
    }

    private boolean isSectionBorder(int position) {
        return position < mItems.length && mItems[position] == null;
    }

    private View getSectionBorder(View convertView, ViewGroup parent) {
        if (convertView == null || convertView.getTag() != ViewType.SECTION_BORDER) {
            convertView = mInflater.inflate(mSectionBorderResource, parent, false);
        }
        convertView.setClickable(true);

        // Manually manage the different types of views, since Spinners don't
        // ordinarily support multiple view types and ignore getViewTypeCount().
        convertView.setTag(ViewType.SECTION_BORDER);

        return convertView;
    }

    private void resetClickable(View convertView) {
        if (convertView != null) {
            convertView.setClickable(false);
        }
    }
}
