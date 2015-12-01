// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
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

/**
 * Snackbars provide lightweight feedback about an operation by showing a brief message at the
 * bottom of the screen. Snackbars can contain an action button with an OnClickListener.
 * Can be auto dismissed by a time out values in seconds and can be prioritized.
 */
public class SnackBar {

    private final ViewGroup mTargetParent;
    private final Context mContext;
    private ExpandableListView mList;
    private SnackBarListAdapter adapter;
    private ArrayList<HashMap<String, String>> groups;
    private ArrayList<ArrayList<HashMap<String, String>>> children;
    private int mMessageId;
    private TreeMap<MessageKey, Message> mMessagesList;


    public SnackBar(ViewGroup parent) {
        mTargetParent = parent;
        mContext = parent.getContext();

        mMessagesList= new TreeMap<>();
        children = new ArrayList<>();
        buildList();
    }

    /**
     * Show the SnackBar with an slide up animation if hidden.
     */
    public void show() {
        // TODO: discover why does not animate on the first time
        if (mList.getVisibility() != View.VISIBLE) {
            animate(View.VISIBLE);
        }
    }

    /**
     * Hide the SnackBar with a slide down animation if visible.
     */
    public void hide() {
        animate(View.GONE);
    }

    /**
     * Play the slide up / down animation depending on the list current visibility.
     * @param visibility Defines which of the animations will be executed (up or down). Possible
     *                   values are {@link View#VISIBLE} or {@link View#GONE}
     */
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

    /**
     * Wrapper to the full message method {@link #message(String, String, View.OnClickListener, int,
     * boolean, int)}.
     * The message won't have action button, it's priority will be 999, will be dismissible and
     * won't have timer.
     * @param message The message String.
     * @return the id of the message.
     */
    public int message(String message) {
        return message(message, null, null, 999, true, 0);
    }

    /**
     * Wrapper to the full message method {@link #message(String, String, View.OnClickListener, int,
     * boolean, int)}.
     * The message won't have action button, will be dismissible and won't have timer.
     * @param message The message String.
     * @param priority The priority of the message. The param is a int and the lower the number the
     *                 higher priority the message has. 0 is the highest.
     * @return the id of the message.
     */
    public int message(String message, int priority) {
        return message(message, null, null, priority, true, 0);
    }

    /**
     * Wrapper to the full message method {@link #message(String, String, View.OnClickListener, int,
     * boolean, int)}.
     * The message will be dismissible and won't have timer.
     * @param message The message String.
     * @param actionMessage The label for the action button.
     * @param actionOnClick The View.OnClickListener for the action button.
     * @param priority The priority of the message. The param is a int and the lower the number the
     *                 higher priority the message has. 0 is the highest.
     * @return the id of the message
     */
    public int message(String message, String actionMessage, View.OnClickListener actionOnClick,
                       int priority){
        return message(message, actionMessage, actionOnClick, priority, true, 0);
    }

    /**
     * Add to the list and display a new message. This is the method that should be used to
     * display messages and it's being called by {@link BaseActivity#snackBar}.
     * @param message The message String.
     * @param actionMessage The label for the action button.
     * @param actionOnClick The View.OnClickListener for the action button.
     * @param priority The priority of the message. The param is a int and the lower the number the
     *                 higher priority the message has. 0 is the highest.
     * @param isDismissible if true the message will have a X button to remove it self from the
     *                      list.
     * @param secondsToTimeOut Number of seconds to message auto dismiss. 0 to never.
     * @return the id of the message.
     */
    public int message(String message, String actionMessage, View.OnClickListener actionOnClick,
                        int priority, boolean isDismissible, int secondsToTimeOut){
        mMessageId++;
        MessageKey key = new MessageKey(mMessageId, priority);
        Message value = new Message(key, message, actionMessage, actionOnClick, isDismissible);
        mMessagesList.put(key, value);
        updateList();
        if(secondsToTimeOut > 0) {
            setTimer(key, secondsToTimeOut);
        }
        if (mMessagesList.size() == 0) {
            hide();
        } else {
            show();
        }
        return mMessageId;
    }

    /**
     * Sets the auto-dismiss timer to the message given it's key and seconds to dismiss.
     * @param key The message key.
     * @param seconds Seconds until dismiss.
     */
    private void setTimer(final MessageKey key, int seconds) {
        int limit = seconds*1000;
        new CountDownTimer(limit, limit) {
            @Override public void onTick(long millisUntilFinished) {}

            @Override public void onFinish() {
                mMessagesList.remove(key);
                updateList();
            }
        }.start();
    }

    /**
     * Programmatically dismiss a message by it's id.
     * @param id The message id.
     */
    public void dismiss(int id) {
        dismiss(getKey(id));
    }

    /**
     * Programmatically dismiss a message by a MessageKey Object
     * @param key The message Key.
     */
    public void dismiss(MessageKey key) {
        mMessagesList.remove(key);
        updateList();
    }

    /**
     * Find message Key by it's id value.
     * @param id The id of the message.
     * @return The MessageKey of the message.
     */
    private MessageKey getKey(int id) {
        MessageKey theKey = null;
        for(Map.Entry<MessageKey, Message> entry : mMessagesList.entrySet()) {
            MessageKey key = entry.getKey();
            if (key.id == id) {
                theKey = key;
                break;
            }
        }
        return theKey;
    }

    /**
     * Rearrange the message data and notify the change to the adapter so it can redraw the
     * ExpandableListView.
     */
    private void updateList() {
        groups.clear();
        children.clear();
        children.add(new ArrayList<HashMap<String, String>>());
        for(Map.Entry<MessageKey, Message> entry : mMessagesList.entrySet()) {
            Message value = entry.getValue();

            if (groups.size() == 0) {
                addToHashMap(value, groups);
            } else {
                addToHashMap(value, children.get(0));
            }
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * The main message list {@code mMessagesList} values are added to ArrayLists to be used by the
     * SnackBarListAdapter.
     * @param m The message to be stored.
     * @param a The group or children ArrayList.
     */
    private void addToHashMap(Message m, ArrayList<HashMap<String, String>> a) {
        HashMap<String, String> newMessage = new HashMap<>();
        newMessage.put("id", Integer.toString(m.key.id));
        newMessage.put("priority", Integer.toString(m.key.priority));
        newMessage.put("messages", m.message);
        if (m.actionString != null) {
            newMessage.put("actions", m.actionString);
        }
        a.add(newMessage);
    }

    /**
     * Initiates the SnackBar ExpandableListView.
     */
    private void buildList() {
        mList = new ExpandableListView(mContext);
        mList.setId(R.id.snackbar);
        setListAppearance();
        mList.setAdapter(generateAdapter());
        mTargetParent.addView(mList);
        hide();
    }

    /**
     * Sets the SnackBar ExpandableListView appearance.
     */
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

    /**
     * Instantiates the SnackBarListAdapter with no messages.
     * @return SnackBarListAdapter
     */
    private SnackBarListAdapter generateAdapter() {
        groups = new ArrayList<>();
        children = new ArrayList<>();
        children.add(new ArrayList<HashMap<String, String>>());

        adapter = new SnackBarListAdapter(
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

    /**
     * The SnackBar ExpandableListView Adapter responsible to handling the message data.
     */
    private final class SnackBarListAdapter extends SimpleExpandableListAdapter {

        public SnackBarListAdapter(Context context, List<? extends Map<String, ?>> groupData, int
            groupLayout, String[] groupFrom, int[] groupTo, List<? extends List<? extends
            Map<String, ?>>> childData, int childLayout, String[] childFrom, int[] childTo) {
            super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout,
                childFrom, childTo);
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            View newView = super.getChildView(groupPosition, childPosition, isLastChild,
                convertView, parent);
            addHandlers(newView);
            return newView;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                                ViewGroup parent) {
            View newView = super.getGroupView(groupPosition, isExpanded, convertView, parent);
            addHandlers(newView);
            return newView;
        }

        /**
         * This method is being called by {@code #getChildView} and {@code #getGroupView} to
         * customize the list items depending on the message specifications.
         * It add the click handlers to the buttons or hide them if they aren't being used.
         * @param newView the inflated view by {@code #getChildView} and {@code #getGroupView}
         */
        private void addHandlers(View newView) {
            TextView idView = (TextView) newView.findViewById(R.id.snackbar_id);
            final String id = idView.getText().toString();
            TextView priorityView = (TextView) newView.findViewById(R.id.snackbar_priority);
            final String priority = priorityView.getText().toString();
            Message m = mMessagesList.get(new MessageKey(Integer.parseInt(id), Integer.parseInt(priority)));

            // Set action handler
            TextView actionButton = (TextView) newView.findViewById(R.id.snackbar_action);
            if ((m.actionHandler != null) && (!m.actionString.isEmpty())) {
                actionButton.setOnClickListener(m.actionHandler);
                actionButton.setVisibility(View.VISIBLE);
            } else {
                actionButton.setVisibility(View.GONE);
            }

            // Set Dismiss handler
            ImageView dismissButton = (ImageView) newView.findViewById(R.id.snackbar_dismiss);
            if (m.isDismissible) {
                dismissButton.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        dismiss(
                            new MessageKey(Integer.parseInt(id), Integer.parseInt(priority))
                        );
                    }
                });
                dismissButton.setVisibility(View.VISIBLE);
            } else {
                dismissButton.setVisibility(View.GONE);
            }
        }
    }

    /**
     * The key of the {@code TreeMap} used to storing the messages.
     * Helps the {@code TreeMap} maintain it's balance using priority as a parameter.
     */
    private class MessageKey implements Comparable<MessageKey> {

        protected int id;
        protected int priority;

        public MessageKey(int id, int priority){
            this.id = id;
            this.priority = priority;
        }

        /**
         * Used to TreeMap balancing and ordering by priority but also used by TreeMap get() method.
         * It matches the key with the same id disregarding it's priority.
         * @param another The key to compare
         * @return 0 to equal, > 0 to greater than and < 0 to less than.
         */
        @Override public int compareTo(@NonNull MessageKey another) {
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

    /**
     * The message information.
     */
    private class Message {

        protected MessageKey key;
        protected String message;
        protected String actionString;
        protected View.OnClickListener actionHandler;
        protected boolean isDismissible;

        public Message(MessageKey key, String message, String actionString,
                       View.OnClickListener handler, boolean isDismissible) {
            this.key = key;
            this.message = message;
            this.actionString = actionString;
            this.actionHandler = handler;
            this.isDismissible = isDismissible;
        }

    }

}