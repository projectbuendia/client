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
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

import org.projectbuendia.client.R;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;


public class SnackBar {

    private final ViewGroup mTargetParent;
    private final Context mContext;
    private ExpandableListView mList;
    private ArrayList groups;
    private ArrayList children;
    private SimpleExpandableListAdapter adapter;

    public SnackBar(ViewGroup parent) {
        mTargetParent = parent;
        mContext = parent.getContext();
        children = new ArrayList();
        buildList();
    }

    public void show() {
        // TODO: discover why does not animate on the first time
        animate(View.VISIBLE);
    }

    public void hide() {
        animate(View.GONE);
    }

    private void animate(int visibility) {
        TranslateAnimation animate = new TranslateAnimation(0, 0,
            visibility == View.VISIBLE ? mList.getHeight() : 0,
            visibility == View.GONE ? mList.getHeight() : 0
        );
        animate.setDuration(700);
        animate.setFillAfter(true);
        mList.startAnimation(animate);
        mList.setVisibility(visibility);
    }

    public void message(String message) {
        message(message, null, null);
    }

    public void message(String message, String actionMessage, View.OnClickListener actionOnClick){
        if (groups.size() == 0) {
            HashMap group = new HashMap();
            group.put("messages", message);
            if (actionMessage != null) {
                group.put("actions", actionMessage);
            }
            groups.add(group);
        } else {
            HashMap child = new HashMap();
            child.put("messages", message);
            if (actionMessage != null) {
                child.put("actions", actionMessage);
            }
            if (children.size() == 0) {
                children.add(new ArrayList());
            }
            ((ArrayList) children.get(0)).add(child);
        }
        adapter.notifyDataSetChanged();
        show();
    }

    private void buildList() {
        mList = new ExpandableListView(mContext);
        setListAppearance();
        mList.setAdapter(generateAdapter());
        mTargetParent.addView(mList);
        hide();
    }

    private void setListAppearance(){
        mList.setDivider(null);
        mList.setChildDivider(null);
        Drawable icon = mContext.getResources().getDrawable(R.drawable.snackbar_group_indicator);
        mList.setGroupIndicator(icon);
        mList.setIndicatorBounds(
            1200 - Utils.getPixelFromDips(40),
            1200 - Utils.getPixelFromDips(10)
        );
    }

    private SimpleExpandableListAdapter generateAdapter(){
        groups = new ArrayList();
        children = new ArrayList();
        children.add(new ArrayList());

        adapter = new SimpleExpandableListAdapter(
            mContext,
            groups,                            // Creating group List.
            R.layout.snackbar_group,                // Group item layout XML.
            new String[] {"messages", "actions"},   // the key of group item.
            new int[] {                             // ID of each group item.-Data under the key goes into this TextView.
                R.id.snackbar_message,
                R.id.snackbar_group_action
            },
            children,                            // childData describes second-level entries.
            R.layout.snackbar_item,                 // Layout for sub-level entries(second level).
            new String[] {"messages", "actions"},   // Keys in childData maps to display.
            new int[] {                             // Data under the keys above go into these TextViews.
                R.id.snackbar_message,
                R.id.snackbar_action
            }
        );
        return adapter;
    }

//    private void childData() {
//        children = new ArrayList();
//        ArrayList secList = new ArrayList();
//        for( int n = 0 ; n < 3 ; n++ ) {
//            HashMap child = new HashMap();
//            child.put( "messages", "Wifi disabled " + n );
//            child.put("actions", "Do Something");
//            secList.add(child);
//        }
//        children.add(secList);
//
//        //return children;
//        //return new ArrayList();
//    }
//
//    private void groupData() {
//        groups = new ArrayList();
//        HashMap group = new HashMap();
//        group.put("messages", "Could not connect to server!");
//        group.put("actions", "Do Something");
//        groups.add(group);
//        //return groups;
//        //return new ArrayList();
//    }

//    public void teste() {
//        TextView actionButton = (TextView) mList.findViewById(R.id.snackbar_group_action);
//        if (actionButton != null) {
//            actionButton.setOnClickListener(new View.OnClickListener() {
//                @Override public void onClick(View v) {
//                    Toast.makeText(
//                        mContext,
//                        "lixo",
//                        Toast.LENGTH_LONG
//                    ).show();
//                }
//            });
//        }
//    }
//
//    public void teste2() {
//        TextView actionButton = (TextView) mList.findViewById(R.id.snackbar_action);
//        if (actionButton != null) {
//            mList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
//                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
//                                            int
//                                                childPosition, long id) {
//                    Toast.makeText(
//                        mContext,
//                        ((TextView) v.findViewById(R.id.snackbar_message)).getText(),
//                        Toast.LENGTH_LONG
//                    ).show();
//                    return false;
//                }
//            });
//        }
//    }

}