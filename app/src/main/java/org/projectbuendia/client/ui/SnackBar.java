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
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class SnackBar {

    private final ViewGroup mTargetParent;
    private final Context mContext;
    private ExpandableListView mList;
    private SnackBarListAdapter adapter;
    private ArrayList groups;
    private ArrayList children;
    private int mMessageId;
    private TreeMap<MessageKey, Message> mMessagesList;


    public SnackBar(ViewGroup parent) {
        mTargetParent = parent;
        mContext = parent.getContext();

        mMessagesList= new TreeMap<>();
        children = new ArrayList();
        buildList();
    }

    public void show() {
        // TODO: discover why does not animate on the first time
        if (mList.getVisibility() != View.VISIBLE) {
            animate(View.VISIBLE);
        }
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

    public int message(String message) {
        return message(message, null, null, 999);
    }

    public int message(String message, int priority) {
        return message(message, null, null, priority);
    }

    public int message(String message, String actionMessage, View.OnClickListener actionOnClick,
                        int priority){
        MessageKey key = new MessageKey(++mMessageId, priority);
        Message value = new Message(key, message, actionMessage, actionOnClick);
        mMessagesList.put(key, value);
        updateList();
        if (mMessagesList.size() == 0) {
            hide();
        } else {
            show();
        }
        return mMessageId;
    }

    public void dismiss(MessageKey key) {
        mMessagesList.remove(key);
        updateList();
    }

    private void updateList() {
        groups.clear();
        children.clear();
        children.add(new ArrayList());
        for(Map.Entry<MessageKey, Message> entry : mMessagesList.entrySet()) {
            Message value = entry.getValue();

            if (groups.size() == 0) {
                addToHashMap(value, groups);
            } else {
                addToHashMap(value, (ArrayList) children.get(0));
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void addToHashMap(Message m, ArrayList a) {
        HashMap newMessage = new HashMap();
        newMessage.put("id", Integer.toString(m.key.id));
        newMessage.put("priority", Integer.toString(m.key.priority));
        newMessage.put("messages", m.message);
        if (m.actionString != null) {
            newMessage.put("actions", m.actionString);
        }
        a.add(newMessage);
    }

    private void buildList() {
        mList = new ExpandableListView(mContext);
        setListAppearance();
        mList.setAdapter(generateAdapter());
        mTargetParent.addView(mList);
        hide();
    }

    private void setListAppearance() {
        mList.setDivider(null);
        mList.setChildDivider(null);
        Drawable icon = mContext.getResources().getDrawable(R.drawable.snackbar_group_indicator);
        mList.setGroupIndicator(icon);
        mList.setIndicatorBounds(
            1200 - Utils.getPixelFromDips(60),
            1200 - Utils.getPixelFromDips(32)
        );
    }

    private SnackBarListAdapter generateAdapter() {
        groups = new ArrayList();
        children = new ArrayList();
        children.add(new ArrayList());

        adapter = new SnackBarListAdapter(
            this,
            mContext,
            groups,                                                   // Creating group List.
            R.layout.snackbar_group,                                  // Group item layout XML.
            new String[] {"id", "priority", "messages", "actions"},   // the key of group item.
            new int[] {                                               // Data under the key goes into this TextView.
                R.id.snackbar_id,
                R.id.snackbar_priority,
                R.id.snackbar_message,
                R.id.snackbar_action
            },
            children,                                                 // childData describes second-level entries.
            R.layout.snackbar_item,                                   // Layout for sub-level entries(second level).
            new String[] {"id", "priority", "messages", "actions"},   // Keys in childData maps to display.
            new int[] {                                               // Data under the keys above go into these TextViews.
                R.id.snackbar_id,
                R.id.snackbar_priority,
                R.id.snackbar_message,
                R.id.snackbar_action
            }
        );
        return adapter;
    }

    private class SnackBarListAdapter extends SimpleExpandableListAdapter {

        private SnackBar snackBar;

        public SnackBarListAdapter(SnackBar snackBar, Context context, List<? extends Map<String, ?>> groupData, int
            groupLayout, String[] groupFrom, int[] groupTo, List<? extends List<? extends
            Map<String, ?>>> childData, int childLayout, String[] childFrom, int[] childTo) {
            super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout,
                childFrom, childTo);
            this.snackBar = snackBar;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            View newView = super.getChildView(groupPosition, childPosition, isLastChild,
                convertView, parent);
            addActonHandler(newView);
            addDismissHandler(newView);
            return newView;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                                ViewGroup parent) {
            View newView = super.getGroupView(groupPosition, isExpanded, convertView, parent);
            addActonHandler(newView);
            addDismissHandler(newView);
            return newView;
        }

        private void addActonHandler(View newView) {
            TextView idView = (TextView) newView.findViewById(R.id.snackbar_id);
            String id = idView.getText().toString();
            TextView priorityView = (TextView) newView.findViewById(R.id.snackbar_priority);
            String priority = priorityView.getText().toString();
            Message m = mMessagesList.get(new MessageKey(Integer.parseInt(id), Integer.parseInt(priority)));
            TextView actionButton = (TextView) newView.findViewById(R.id.snackbar_action);
            String action = actionButton.getText().toString();
            if ((actionButton != null) && (!action.isEmpty())) {
                if((m != null) && (m.actionHandler != null)){
                    actionButton.setOnClickListener(m.actionHandler);
                }
            }
        }

        private void addDismissHandler(View newView) {
            TextView idView = (TextView) newView.findViewById(R.id.snackbar_id);
            final String id = idView.getText().toString();
            TextView priorityView = (TextView) newView.findViewById(R.id.snackbar_priority);
            final String priority = priorityView.getText().toString();
            ImageView dismissButton = (ImageView) newView.findViewById(R.id.snackbar_dismiss);
            dismissButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    snackBar.dismiss(
                        new MessageKey(Integer.parseInt(id), Integer.parseInt(priority))
                    );
                }
            });
        }
    }

    private class MessageKey implements Comparable<MessageKey> {

        protected int id;
        protected int priority;

        public MessageKey(int id, int priority){
            this.id = id;
            this.priority = priority;
        }

        @Override public int compareTo(MessageKey another) {
            int equal = 0;
            int result;
            int idCompare = Integer.compare(this.id, another.id);
            if (idCompare == equal) {
                result = equal;
            } else {
                int priorityCompare = Integer.compare(this.priority, another.priority);
                if (priorityCompare == equal) {
                    result = idCompare;
                } else {
                    result = priorityCompare;
                }
            }
            return result;
        }
    }

    private class Message {

        protected MessageKey key;
        protected String message;
        protected String actionString;
        protected View.OnClickListener actionHandler;

        public Message(MessageKey key, String message, String actionString,
                       View.OnClickListener handler) {
            this.key = key;
            this.message = message;
            this.actionString = actionString;
            this.actionHandler = handler;
        }

    }

}