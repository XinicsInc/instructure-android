/*
 * Copyright (C) 2016 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.instructure.candroid.fragment;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.instructure.candroid.R;
import com.instructure.candroid.activity.ParentActivity;
import com.instructure.candroid.adapter.NotificationListRecyclerAdapter;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.pageview.PageViewUrl;
import com.instructure.interactions.Navigation;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.interfaces.NotificationAdapterToFragmentCallback;
import com.instructure.candroid.util.FragUtils;
import com.instructure.candroid.util.RouterUtils;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Conversation;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Group;
import com.instructure.canvasapi2.models.StreamItem;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.canvasapi2.models.Tab;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.pandarecycler.PandaRecyclerView;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.ViewStyler;

@PageView
public class NotificationListFragment extends ParentFragment {

    //View
    private View mRootView;
    private View mEditOptions;
    private Toolbar mToolbar;

    private NotificationAdapterToFragmentCallback<StreamItem> mAdapterToFragmentCallback;
    private NotificationListRecyclerAdapter mRecyclerAdapter;

    private OnNotificationCountInvalidated onNotificationCountInvalidated;

    public interface OnNotificationCountInvalidated {
        void invalidateNotificationsCount();
    }

    @PageViewUrl
    @SuppressWarnings("unused")
    private String makePageViewUrl() {
        String url = ApiPrefs.getFullDomain();
        if (getCanvasContext().getType() == CanvasContext.Type.USER) return url;
        return url + getCanvasContext().toAPIString();
    }

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.MASTER; }

    @Override
    @NonNull
    public String title() {
        if(navigationContextIsCourse()) return getString(R.string.homePageIdForNotifications);
        else return getString(R.string.notifications);
    }

    @Override
    public boolean navigationContextIsCourse() {
        if(getCanvasContext() instanceof Course || getCanvasContext() instanceof Group) {
            return true;
        }
        return false;
    }

    public String getTabId() {
        return Tab.NOTIFICATIONS_ID;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(this, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = getLayoutInflater().inflate(R.layout.fragment_list_notification, container, false);
        mToolbar = mRootView.findViewById(R.id.toolbar);
        mAdapterToFragmentCallback = new NotificationAdapterToFragmentCallback<StreamItem>() {
            @Override
            public void onRowClicked(StreamItem streamItem, int position, boolean isOpenDetail) {
                mRecyclerAdapter.setSelectedPosition(position);
                Navigation navigation = getNavigation();
                if(navigation != null){
                    onRowClick(streamItem, isOpenDetail);
                }
            }
            @Override
            public void onRefreshFinished() {
                setRefreshing(false);
                mEditOptions.setVisibility(View.GONE);
            }

            @Override
            public void onShowEditView(boolean isVisible) {
                mEditOptions.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onShowErrorCrouton(int message) {
                showToast(message);
            }
        };
        mRecyclerAdapter = new NotificationListRecyclerAdapter(getContext(), getCanvasContext(), onNotificationCountInvalidated, mAdapterToFragmentCallback);
        configureRecyclerView(mRootView, getContext(), mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView);

        PandaRecyclerView pandaRecyclerView = mRootView.findViewById(R.id.listView);
        pandaRecyclerView.setSelectionEnabled(false);
        configureViews(mRootView);

        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mToolbar.setTitle(title());
    }

    @Override
    public void applyTheme() {
        CanvasContext canvasContext = getCanvasContext();
        if(canvasContext instanceof Course || canvasContext instanceof Group) {
            PandaViewUtils.setupToolbarBackButton(mToolbar, this);
            ViewStyler.themeToolbar(getActivity(), mToolbar, canvasContext);
        } else {
            Navigation navigation = getNavigation();
            if(navigation != null) navigation.attachNavigationDrawer(this, mToolbar);
            //Styling done in attachNavigationDrawer
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configureRecyclerView(mRootView, getContext(), mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView);
    }

    /* fixme not sure Unread count is even used anymore, but the code is still here if it is ever put in again
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            onNotificationCountInvalidated = (OnNotificationCountInvalidated) activity;
        } catch (ClassCastException e) {
            onNotificationCountInvalidated = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //If they've selected a message and come back, check to see how many unread there are now; it may have changed.
        if(onNotificationCountInvalidated != null) {
            onNotificationCountInvalidated.invalidateNotificationsCount();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter("updateUnreadCount"));

    }

    @Override
    public void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    // handler for received Intents for the "updateUnreadCount" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Don't need any data from the intent, we'll just set the selected item as unread
            // and update the list. We could also reloadData to pull the actual data from the
            // server which would have this item marked as unread. But that also clears the
            // adapter and if the user was scrolled down it would send them back up to the top
            // of the list

            StreamItem item = getSelectedItem();
            item.setReadState(true);
            notifyDataSetChanged();
        }
    };
    */

    // region Handle StreamItem

    public boolean onRowClick(StreamItem streamItem, boolean closeSlidingPane) {
        //TODO: pass should show first to addFragmentForStreamItem so we only open the sliding pane for the actual click events

        //if the course/group is null, this will crash. This happened in one case because the api returned an assignment for a course
        //that is concluded.
        if(streamItem.getCanvasContext() == null && streamItem.getContextType() != CanvasContext.Type.USER) {
            if(streamItem.getContextType() == CanvasContext.Type.COURSE) {
                showToast(R.string.could_not_find_course);
            } else if(streamItem.getContextType() == CanvasContext.Type.GROUP) {
                showToast(R.string.could_not_find_group);
            }
            return false;
        }
        addFragmentForStreamItem(streamItem, (ParentActivity)getActivity(), false);

        return true;
    }

    public static DialogFragment addFragmentForStreamItem(StreamItem streamItem, FragmentActivity activity, boolean fromWidget){
        ParentFragment fragment = null;

        if(streamItem == null || activity == null){
            return null;
        }

        String unsupportedLabel = null;

        switch (streamItem.getType()) {
            case SUBMISSION:
                if (streamItem.getAssignment() == null && streamItem.getCanvasContext() instanceof Course) {
                    fragment = FragUtils.getFrag(AssignmentFragment.class, AssignmentFragment.Companion.createBundle(streamItem.getCanvasContext(), streamItem.getAssignmentId()));
                } else if(streamItem.getAssignment() != null && streamItem.getCanvasContext() instanceof Course){
                    //add an empty submission with the grade to the assignment so that we can see the score.
                    Submission emptySubmission = new Submission();
                    emptySubmission.setGrade(streamItem.getGrade());
                    streamItem.getAssignment().setSubmission(emptySubmission);
                    fragment = FragUtils.getFrag(AssignmentFragment.class, AssignmentFragment.Companion.createBundle((Course)streamItem.getCanvasContext(), streamItem.getAssignment()));
                }
                break;
            case ANNOUNCEMENT:
                if(streamItem.getCanvasContext() != null) {
                    fragment = FragUtils.getFrag(DiscussionDetailsFragment.class, DiscussionDetailsFragment.makeBundle(streamItem.getCanvasContext(), streamItem.getDiscussionTopicId(), true));
                }
                break;
            case CONVERSATION:
                Conversation conversation = streamItem.getConversation();
                if (conversation != null) {
                    //Check to see if the conversation has been deleted.
                    if (conversation.isDeleted()) {
                        Toast.makeText(activity, R.string.deleteConversation, Toast.LENGTH_SHORT).show();
                        return null;
                    }
                    Bundle extras = InboxConversationFragment.createBundle(conversation, 0, null);
                    fragment = FragUtils.getFrag(InboxConversationFragment.class, extras);
                }
                break;
            case DISCUSSION_TOPIC:
                if(streamItem.getCanvasContext() != null) {
                    fragment = FragUtils.getFrag(DiscussionDetailsFragment.class, DiscussionDetailsFragment.makeBundle(streamItem.getCanvasContext(), streamItem.getDiscussionTopicId(), false));
                }
                break;
            case MESSAGE:
                if(streamItem.getAssignmentId() > 0) {
                    if(streamItem.getCanvasContext() != null) {
                        fragment = FragUtils.getFrag(AssignmentFragment.class, AssignmentFragment.Companion.createBundle(activity, streamItem.getCanvasContext(), streamItem.getAssignmentId(), streamItem));
                    }
                } else{
                    fragment = FragUtils.getFrag(UnknownItemFragment.class, UnknownItemFragment.createBundle(streamItem.getCanvasContext(), streamItem));
                }
                break;
            case COLLABORATION:
                if(streamItem.getCanvasContext() != null) {
                    unsupportedLabel = activity.getString(R.string.collaborations);
                    fragment = UnSupportedTabFragment.createFragment(UnSupportedTabFragment.class, UnSupportedTabFragment.createBundle(streamItem.getCanvasContext(), Tab.COLLABORATIONS_ID, R.string.collaborations));
                }
                break;
            case CONFERENCE:
                if(streamItem.getCanvasContext() != null) {
                    unsupportedLabel = activity.getString(R.string.conferences);
                    fragment = UnSupportedTabFragment.createFragment(UnSupportedTabFragment.class, UnSupportedTabFragment.createBundle(streamItem.getCanvasContext(), Tab.CONFERENCES_ID, R.string.conferences));
                }
                break;
            default:
                if(streamItem.getCanvasContext() != null) {
                    unsupportedLabel = streamItem.getType().toString();
                    fragment = FragUtils.getFrag(UnSupportedFeatureFragment.class, UnSupportedFeatureFragment.createBundle(streamItem.getCanvasContext(), unsupportedLabel, streamItem.getUrl()));
                }
                break;
        }

        if (unsupportedLabel != null) {
            if(activity instanceof Navigation) {
                ((Navigation)activity).addFragment(fragment);
            }
        } else {

            if(activity instanceof Navigation && fragment != null) {
                ((Navigation)activity).addFragment(fragment);
            }

            if(fromWidget){
                if(streamItem.getUrl() != null){
                    RouterUtils.routeUrl(activity, streamItem.getUrl(), false);
                }else{
                    RouterUtils.routeUrl(activity, streamItem.getHtmlUrl(), false);
                }
            }
        }

        return null;
    }

    // endregion

    // region Edit view

    public void configureViews(View rootView) {
        mEditOptions = rootView.findViewById(R.id.editOptions);

        Button confirmButton = (Button) rootView.findViewById(R.id.confirmButton);
        confirmButton.setText(getString(R.string.delete));
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecyclerAdapter.confirmButtonClicked();
            }
        });

        Button cancelButton = (Button) rootView.findViewById(R.id.cancelButton);
        cancelButton.setText(getString(R.string.cancel));
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecyclerAdapter.cancelButtonClicked();
            }
        });
    }

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);
        if (extras == null) {return;}

        if(extras.containsKey(Const.SELECTED_ITEM)){
            StreamItem streamItem = (StreamItem)extras.getSerializable(Const.SELECTED_ITEM);
            setDefaultSelectedId(streamItem.getId());
        }
    }

    @Override
    public boolean allowBookmarking() {
        if(getCanvasContext() instanceof Course || getCanvasContext() instanceof Group) {
            return true;
        }
        return false;
    }
}
