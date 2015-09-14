// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * An {@link ArrayAdapter} that uses null values to indicate 'section' boundaries. Section
 * boundaries are represented by an arbitrary resource (such as a horizontal divider) and are not
 * clickable.
 */
public class SectionedSpinnerAdapter<T> extends ArrayAdapter<T> {
    private final T[] mItems;
    private final int mSectionBorderResource;
    private final LayoutInflater mInflater;

    /**
     * Instantiates a {@link SectionedSpinnerAdapter} with the given resources and contents.
     * @param context               the Application or Activity context
     * @param collapsedResource     the {@link android.graphics.drawable.Drawable} used to display the
     *                              selected adapter item when the list is collapsed
     * @param dropDownResource      the {@link android.graphics.drawable.Drawable} used to display an
     *                              adapter item when the list is expanded
     * @param sectionBorderResource the {@link android.graphics.drawable.Drawable} used to display
     *                              section dividers (null items)
     * @param items                 the contents of the list
     */
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

    /**
     * Returns the type for the view at the given position, where valid types are ordinal values of
     * the {@link ViewType} enum.
     * @param position the position of the view in the adapter
     */
    public int getItemViewType(int position) {
        return isSectionBorder(position)
            ? ViewType.SECTION_BORDER.ordinal()
            : ViewType.LIST_ITEM.ordinal();
    }

    private enum ViewType {
        SECTION_BORDER, LIST_ITEM
    }
}
