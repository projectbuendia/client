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
import android.content.res.Resources;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import org.projectbuendia.client.R;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Snackbars provide lightweight feedback about an operation by showing a brief message at the
 * bottom of the screen. Snackbars can contain an action button with an OnClickListener.
 * Can be auto dismissed by a time out values in seconds and can be prioritized.
 */
public class SnackBar {

    private ViewGroup mTargetParent;
    private Context mContext;
    private SnackBarListView mList;
    private SnackBarListAdapter adapter;
    private int mMessageId;
    private static TreeMap<MessageKey, Message> mMessagesList;


    public SnackBar(ViewGroup parent) {
        mTargetParent = parent;
        mContext = parent.getContext();
        if (mMessagesList == null) {
            mMessagesList = new TreeMap<>();
        }
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
     * Wrapper to the full message method {@link #message(int, int, View.OnClickListener, int,
     * boolean, int)}
     * The message won't have action button, it's priority will be 999, will be dismissible and
     * won't have timer.
     * @param message The message String.
     */
    public void message(@StringRes int message) {
        message(message, 0, null, 999, true, 0);
    }

    /**
     * Wrapper to the full message method {@link #message(int, int, View.OnClickListener, int,
     * boolean, int)}
     * The message won't have action button, will be dismissible and won't have timer.
     * @param message  The message String resource id.
     * @param priority The priority of the message. The param is a int and the lower the number the
     *                 higher priority the message has. 0 is the highest.
     */
    public void message(@StringRes int message, int priority) {
        message(message, 0, null, priority, true, 0);
    }

    /**
     * Wrapper to the full message method {@link #message(int, int, View.OnClickListener, int,
     * boolean, int)}
     * The message will be dismissible and won't have timer.
     * @param message       The message String resource id.
     * @param actionMessage The resource id of the label for the action button.
     * @param actionOnClick The View.OnClickListener for the action button.
     * @param priority      The priority of the message. The param is a int and the lower the
     *                      number the
     *                      higher priority the message has. 0 is the highest.
     */
    public void message(@StringRes int message, @StringRes int actionMessage, View.OnClickListener
        actionOnClick, int priority) {
        message(message, actionMessage, actionOnClick, priority, true, 0);
    }

    /**
     * Add to the list and display a new message. This is the method that should be used to
     * display messages and it's being called by {@link BaseActivity#snackBar}.
     * @param message          The message String resource id.
     * @param actionMessage    The resource id of the label for the action button.
     * @param actionOnClick    The View.OnClickListener for the action button.
     * @param priority         The priority of the message. The param is a int and the lower the
     *                         number the
     *                         higher priority the message has. 0 is the highest.
     * @param isDismissible    if true the message will have a X button to remove it self from the
     *                         list.
     * @param secondsToTimeOut Number of seconds to message auto dismiss. 0 to never.
     */
    public void message(@StringRes int message, @StringRes int actionMessage, View.OnClickListener
        actionOnClick, int priority, boolean isDismissible, int secondsToTimeOut) {
        mMessageId++;
        MessageKey key = new MessageKey(mMessageId, priority);
        Message value = new Message(key, message, actionMessage, actionOnClick, isDismissible);
        addToQueueWithoutDuplicate(key, value);
        adapter.notifyDataSetChanged();
        if (secondsToTimeOut > 0) {
            setTimer(key, secondsToTimeOut);
        }
        if (mMessagesList.size() == 0) {
            hide();
        } else {
            show();
        }
    }

    /**
     * Checks if a message has duplicate
     */
    private void addToQueueWithoutDuplicate(MessageKey key, Message value) {
        Message existingMessage = getMessage(value.message);
        if (existingMessage != null) {
            mMessagesList.remove(existingMessage.key);
        }
        mMessagesList.put(key, value);
    }

    /**
     * Sets the auto-dismiss timer to the message given it's key and seconds to dismiss.
     * @param key     The message key.
     * @param seconds Seconds until dismiss.
     */
    private void setTimer(final MessageKey key, int seconds) {
        int limit = seconds*1000;
        new CountDownTimer(limit, limit) {
            @Override public void onTick(long millisUntilFinished) {
            }

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
        for (Map.Entry<MessageKey, Message> entry : mMessagesList.entrySet()) {
            MessageKey key = entry.getKey();
            if (key.id == id) {
                theKey = key;
                break;
            }
        }
        return theKey;
    }

    private Message getMessage(@StringRes int message){
        Message theMessage = null;
        for (Map.Entry<MessageKey, Message> entry : mMessagesList.entrySet()) {
            MessageKey key = entry.getKey();
            Message value = entry.getValue();
            if (value.message == message) {
                theMessage = value;
            }
        }
        return theMessage;
    }

    /**
     * Initiates the SnackBar ExpandableListView.
     */
    private void buildList() {
        mList = new SnackBarListView(mContext);
        mList.setId(R.id.snackbar);
        setListAppearance();
        adapter = new SnackBarListAdapter(mContext);
        mList.setAdapter(adapter);
        mTargetParent.addView(mList);
        if (mMessagesList.size() > 0) {
            show();
        } else {
            hide();
        }
    }

    /**
     * Sets the SnackBar ExpandableListView appearance.
     */
    private void setListAppearance() {
        mList.setDivider(null);
        mList.setChildDivider(null);
    }

    /**
     * The Custom ExpansibleListView used by the SnackBar
     */
    private final class SnackBarListView extends ExpandableListView {

        public SnackBarListView(Context context) {
            super(context);
        }

        /**
         * Updates the SnackBar with the current messages.
         * @param visibility
         */
        @Override protected void onWindowVisibilityChanged(int visibility) {
            super.onWindowVisibilityChanged(visibility);
            adapter.notifyDataSetChanged();
        }

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
            return (mMessagesList.size() == 0) ? 0 : 1;
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
            View header = getView(convertView, parent, groupPosition);
            TextView count = (TextView) header.findViewById(R.id.snackbar_count);
            View indicator = header.findViewById(R.id.snackbar_indicator);
            if (mMessagesList.size() > 1) {
                indicator.setVisibility(View.VISIBLE);
                count.setText(String.valueOf(mMessagesList.size()));
                count.setVisibility(View.VISIBLE);
            } else {
                indicator.setVisibility(View.INVISIBLE);
                count.setVisibility(View.INVISIBLE);
            }
            return header;
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
            Object[] messagesArray = mMessagesList.values().toArray();

            if (newView == null) {
                newView = mInflater.inflate(R.layout.snackbar_item, parent, false);
            }

            if ((position >= 0) && (position < messagesArray.length)) {
                final Message m = (Message) messagesArray[position];
                Resources res = mContext.getResources();
                String messageString = res.getString(m.message);
                String actionString = res.getString(m.actionString);

                TextView message = (TextView) newView.findViewById(R.id.snackbar_message);
                message.setText(messageString);

                TextView action = (TextView) newView.findViewById(R.id.snackbar_action);
                action.setText(actionString);

                // Set action handler
                if ((m.actionHandler != null) && (!actionString.isEmpty())) {
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

        public MessageKey(@StringRes int id, int priority) {
            this.id = id;
            this.priority = priority;
        }

        /**
         * Used to TreeMap balancing and ordering by priority but also used by TreeMap get() method.
         * It matches the key with the same id disregarding it's priority.
         * The ordering is Priority first, Most recent second. Most recent messages appear on top
         * unless exists a higher priority message.
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
                idCompare = -idCompare; //Reverse the order.
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
        protected int message;
        protected int actionString;
        protected View.OnClickListener actionHandler;
        protected boolean isDismissible;

        public Message(MessageKey key, @StringRes int message, @StringRes int actionString,
                       View.OnClickListener handler, boolean isDismissible) {
            this.key = key;
            this.message = message;
            this.actionString = actionString;
            this.actionHandler = handler;
            this.isDismissible = isDismissible;
        }

    }

}