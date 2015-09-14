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

package org.projectbuendia.client.widgets;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Same as {@link GridLayoutManager}, but sets the height based on the height of the content. This
 * is necessary in order to put the {@link RecyclerView} into a {@link android.widget.ScrollView}.
 */
final class WrapContentGridLayoutManager extends GridLayoutManager {

    public WrapContentGridLayoutManager(
        Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
                          int widthSpec, int heightSpec) {
        final int heightMode = View.MeasureSpec.getMode(heightSpec);
        if (heightMode != View.MeasureSpec.UNSPECIFIED) {
            super.onMeasure(recycler, state, widthSpec, heightSpec);
            return;
        }
        // Do default behaviour for width
        final int widthMode = View.MeasureSpec.getMode(widthSpec);
        final int widthSize = View.MeasureSpec.getSize(widthSpec);
        int width = 0;
        switch (widthMode) {
            case View.MeasureSpec.EXACTLY:
            case View.MeasureSpec.AT_MOST:
                width = widthSize;
                break;
            case View.MeasureSpec.UNSPECIFIED:
            default:
                width = getMinimumWidth();
                break;
        }

        // Compute the total height based on the first column
        int totalHeight = getPaddingTop() + getPaddingBottom();
        for (int i = 0; i < getSpanCount(); ++i) {
            int currentHeight = measureScrapChild(recycler, i,
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                HORIZONTAL);
            totalHeight += currentHeight;
        }

        setMeasuredDimension(width, totalHeight);
    }


    private int measureScrapChild(RecyclerView.Recycler recycler, int position, int widthSpec,
                                  int heightSpec, int queriedDimension) {
        View view = recycler.getViewForPosition(position);
        if (view != null) {
            RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) view.getLayoutParams();
            int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                getPaddingLeft() + getPaddingRight(), p.width);
            int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                getPaddingTop() + getPaddingBottom(), p.height);
            view.measure(childWidthSpec, childHeightSpec);
            int result = (queriedDimension == VERTICAL)
                ? view.getMeasuredWidth()
                : view.getMeasuredHeight();
            recycler.recycleView(view);
            return result;
        } else {
            throw new IllegalArgumentException("invalid position " + position);
        }
    }
}
