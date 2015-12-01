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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.utils.Utils;

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
    private int mMessageId;
    private TreeMap<MessageKey, Message> mMessagesList;


    public SnackBar(ViewGroup parent) {
        mTargetParent = parent;
        mContext = parent.getContext();
        mMessagesList= new TreeMap<>();
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
        adapter.notifyDataSetChanged();
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
                adapter.notifyDataSetChanged();
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
        adapter.notifyDataSetChanged();
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
     * Initiates the SnackBar ExpandableListView.
     */
    private void buildList() {
        mList = new ExpandableListView(mContext);
        mList.setId(R.id.snackbar);
        setListAppearance();
        adapter = new SnackBarListAdapter(mContext);
        mList.setAdapter(adapter);
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
     * The SnackBar ExpandableListView Adapter responsible to handling the message data.
     */
    private final class SnackBarListAdapter extends BaseExpandableListAdapter {

        private LayoutInflater mInflater;

        public SnackBarListAdapter(Context context) {
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override public int getGroupCount() {
            return 1;
        }

        @Override public int getChildrenCount(int groupPosition) {
            return mMessagesList.size() - 1;
        }

        @Override public Object getGroup(int groupPosition) {
            return mMessagesList.values().toArray()[groupPosition];
        }

        @Override public Object getChild(int groupPosition, int childPosition) {
            return mMessagesList.values().toArray()[childPosition + 1];
        }

        @Override public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                                 ViewGroup parent) {
            return getView(convertView, parent, groupPosition);
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            return getView(convertView, parent, (childPosition + 1));
        }

        @Override public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        /**
         * This method is being called by {@code #getChildView} and {@code #getGroupView} to
         * customize the list items depending on the message specifications.
         * It add the click handlers to the buttons or hide them if they aren't being used.
         * @param newView the inflated view by {@code #getChildView} and {@code #getGroupView}
         */
        private View getView(View newView, ViewGroup parent, int position) {
            final Message m = (Message) mMessagesList.values().toArray()[position];

            if (newView == null) {
                newView = mInflater.inflate(R.layout.snackbar_item, parent, false);
            }

            TextView message = (TextView) newView.findViewById(R.id.snackbar_message);
            message.setText(m.message);

            TextView action = (TextView) newView.findViewById(R.id.snackbar_action);
            action.setText(m.actionString);

            // Set action handler
            if ((m.actionHandler != null) && (!m.actionString.isEmpty())) {
                action.setOnClickListener(m.actionHandler);
                action.setVisibility(View.VISIBLE);
            } else {
                action.setVisibility(View.GONE);
            }

            // Set Dismiss handler
            ImageView dismissButton = (ImageView) newView.findViewById(R.id.snackbar_dismiss);
            if (m.isDismissible) {
                dismissButton.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        dismiss(m.key);
                    }
                });
                dismissButton.setVisibility(View.VISIBLE);
            } else {
                dismissButton.setVisibility(View.GONE);
            }
            return newView;
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