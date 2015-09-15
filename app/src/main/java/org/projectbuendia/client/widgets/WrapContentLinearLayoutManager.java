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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;

/**
 * Extension of {@link LinearLayoutManager} that wraps its contents by measuring settings its size
 * to the maximum of all its children.
 * See https://code.google.com/p/android/issues/detail?id=74772
 */
public class WrapContentLinearLayoutManager extends LinearLayoutManager {

    public WrapContentLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    @Override public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
                          int widthSpec, int heightSpec) {
        super.onMeasure(recycler, state, widthSpec, heightSpec);
        final int widthMode = View.MeasureSpec.getMode(widthSpec);
        final int heightMode = View.MeasureSpec.getMode(heightSpec);

        if ((widthMode == View.MeasureSpec.AT_MOST && getOrientation() == VERTICAL)
            || (heightMode == View.MeasureSpec.AT_MOST && getOrientation() == HORIZONTAL)) {

            int unSpecifiedSpec = MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int maxSize = 0;
            for (int i = 0; i < state.getItemCount(); ++i) {
                int currentSize = measureScrapChild(recycler, i,
                    unSpecifiedSpec, unSpecifiedSpec, getOrientation());
                maxSize = Math.max(maxSize, currentSize);
            }

            int widthSize = View.MeasureSpec.getSize(widthSpec);
            int heightSize = View.MeasureSpec.getSize(heightSpec);
            if (getOrientation() == VERTICAL) {
                widthSize = maxSize;
            } else {
                heightSize = maxSize;
            }

            setMeasuredDimension(widthSize, heightSize);
        }
    }

    /**
     * Measures a scrap child that is populated with the data on {@code position} in the adapter,
     * using {@code widthSpec} and {@code heightSpec}. Returns the measured size along the
     * non-scrolling dimension of the RecyclerView, given the RecyclerView's {@code orientation}.
     */
    private int measureScrapChild(RecyclerView.Recycler recycler, int position, int widthSpec,
                                  int heightSpec, int orientation) {
        View view = recycler.getViewForPosition(position);
        if (view != null) {
            RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) view.getLayoutParams();
            int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                getPaddingLeft() + getPaddingRight(), p.width);
            int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                getPaddingTop() + getPaddingBottom(), p.height);
            view.measure(childWidthSpec, childHeightSpec);
            int result = (orientation == VERTICAL)
                ? view.getMeasuredWidth()
                : view.getMeasuredHeight();
            recycler.recycleView(view);
            return result;
        } else {
            throw new IllegalArgumentException("invalid position " + position);
        }
    }
}
