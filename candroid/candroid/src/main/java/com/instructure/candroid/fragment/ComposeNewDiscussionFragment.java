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

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import com.instructure.candroid.R;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.DiscussionManager;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.DiscussionTopicHeader;
import com.instructure.canvasapi2.models.Group;
import com.instructure.canvasapi2.utils.APIHelper;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.ViewStyler;

import retrofit2.Call;
import retrofit2.Response;

public class ComposeNewDiscussionFragment extends ParentFragment {

	private boolean isAnnouncement, isEditing;

	private Toolbar toolbar;
	private EditText title;
	private EditText message;
	private CheckBox threaded;
    private CheckBox publish;

    private DiscussionTopicHeader discussionTopicHeader;    //will be null if we're creating a new discussionTopic

    StatusCallback<DiscussionTopicHeader> discussionTopicHeaderCanvasCallback;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {
        return Placement.DIALOG;
    }

    @Override
    @NonNull
    public String title() {
        return isAnnouncement ? getString(R.string.composeAnnouncement) : getString(R.string.composeDiscussion);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isTablet(getContext())) setStyle(DialogFragment.STYLE_NORMAL, R.style.LightStatusBarDialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = getLayoutInflater().inflate(R.layout.fragment_compose_discussion, container, false);
        toolbar = rootView.findViewById(R.id.toolbar);
        message = rootView.findViewById(R.id.message);
        threaded = rootView.findViewById(R.id.threadedCheckbox);
        publish = rootView.findViewById(R.id.publishCheckbox);
        title = rootView.findViewById(R.id.title);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        applyTheme();
        if (isAnnouncement) {
            threaded.setVisibility(View.GONE);
            publish.setVisibility(View.GONE);
        } else {
            threaded.setVisibility(View.VISIBLE);
            publish.setVisibility(View.VISIBLE);
        }

        if (getCanvasContext() instanceof Course) {
            Course course = (Course) getCanvasContext();
            if (((course.isStudent() && !course.isTeacher()))){
                //Students cannot post draft discussions.
                //We force it checked for when the api call is made and hide the option.
                publish.setChecked(true);
                publish.setVisibility(View.GONE);
            }
        } else if (getCanvasContext() instanceof Group) {
            // Always publish for groups
            publish.setChecked(true);
            publish.setVisibility(View.GONE);
        }

        setUpCallback();

        if(discussionTopicHeader != null) {
            populateViewsWithData();
        } else {
            //clear out views
            initViews();
        }
    }

    @Override
    public void applyTheme() {
        toolbar.setTitle(title());
        setupToolbarMenu(toolbar, isAnnouncement ? R.menu.menu_post_announcement : R.menu.menu_post_discussion);
        PandaViewUtils.setupToolbarCloseButton(toolbar, this);
        ViewStyler.themeToolbar(getActivity(), toolbar, Color.WHITE, Color.BLACK, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if(dialog != null && dialog.getWindow() != null && !isTablet(getActivity())) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onPause() {
        // we only want to save the title and message if the user is creating a new discussion
        if(discussionTopicHeader == null) {
            dataLossPause(title, isAnnouncement ? Const.DATA_LOSS_ANNOUNCEMENT_TITLE : Const.DATA_LOSS_DISCUSSION_TITLE);
            dataLossPause(message, isAnnouncement ? Const.DATA_LOSS_ANNOUNCEMENT_MESSAGE : Const.DATA_LOSS_DISCUSSION_MESSAGE);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(discussionTopicHeader == null) {
            dataLossResume(title, isAnnouncement ? Const.DATA_LOSS_ANNOUNCEMENT_TITLE : Const.DATA_LOSS_DISCUSSION_TITLE);
            dataLossResume(message, isAnnouncement ? Const.DATA_LOSS_ANNOUNCEMENT_MESSAGE : Const.DATA_LOSS_DISCUSSION_MESSAGE);
            dataLossAddTextWatcher(title, isAnnouncement ? Const.DATA_LOSS_ANNOUNCEMENT_TITLE: Const.DATA_LOSS_DISCUSSION_TITLE);
            dataLossAddTextWatcher(message, isAnnouncement ? Const.DATA_LOSS_ANNOUNCEMENT_MESSAGE : Const.DATA_LOSS_DISCUSSION_MESSAGE);
        }
    }

    private void populateViewsWithData() {
        title.setText(discussionTopicHeader.getTitle());
        message.setText(Html.fromHtml(discussionTopicHeader.getMessage()));
        if(discussionTopicHeader.getType() == DiscussionTopicHeader.DiscussionType.THREADED) {
            threaded.setChecked(true);
        }
    }

    private void initViews() {
        if(title.getText() != null) {
            title.getText().clear();
        }
        if(message.getText() != null) {
            message.getText().clear();
        }
        threaded.setChecked(false);
    }

    public void setUpCallback(){
        discussionTopicHeaderCanvasCallback = new StatusCallback<DiscussionTopicHeader>() {
            @Override
            public void onResponse(@NonNull Response<DiscussionTopicHeader> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                if(!apiCheck()) {
                    return;
                }
                if (response.body().unauthorized) {
                    String message;
                    if (isAnnouncement) {
                        message = getResources().getString(R.string.notAuthorizedAnnouncement);
                    } else {
                        message = getResources().getString(R.string.notAuthorizedDiscussion);
                    }

                    showToast(message);
                } else {

                    //Let the discussion list know to update itself
                    String message;
                    if (isAnnouncement) {
                        message = getResources().getString(R.string.postAnnouncementSuccess);
                    } else {
                        if(discussionTopicHeader == null) {
                            //this is a new discussion, check if it's published
                            if(response.body().isPublished()) {
                                message = getResources().getString(R.string.postDiscussionSuccess);
                            } else {
                                message = getResources().getString(R.string.draftDiscussionSuccess);
                            }
                        }
                        else {
                            //we're updating an existing discussion
                            message = getResources().getString(R.string.updateDiscussionSuccess);
                        }
                    }

                    showToast(message);
                }

                if (ComposeNewDiscussionFragment.this.title != null && ComposeNewDiscussionFragment.this.message != null) {
                    ComposeNewDiscussionFragment.this.title.setText("");
                    ComposeNewDiscussionFragment.this.message.setText("");
                }

                dataLossDeleteStoredData(isAnnouncement ? Const.DATA_LOSS_ANNOUNCEMENT_TITLE : Const.DATA_LOSS_DISCUSSION_TITLE);
                dataLossDeleteStoredData(isAnnouncement ? Const.DATA_LOSS_ANNOUNCEMENT_MESSAGE : Const.DATA_LOSS_DISCUSSION_MESSAGE);

                closeKeyboard();
                Intent refreshIntent = new Intent(Const.REFRESH);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(refreshIntent);
                getActivity().onBackPressed();
            }

            @Override
            public void onFail(@Nullable Call<DiscussionTopicHeader> call, @NonNull Throwable error, @Nullable Response response) {
                String message;
                if(isAnnouncement){
                    message = getResources().getString(R.string.errorPostingAnnouncement);
                } else {
                    message = getResources().getString(R.string.errorPostingDiscussion);
                }

                showToast(message);
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.menu_post_announcement || item.getItemId() == R.id.menu_post_discussion) {
            if(!APIHelper.hasNetworkConnection()) {
                Toast.makeText(getContext(), getContext().getString(R.string.notAvailableOffline), Toast.LENGTH_SHORT).show();
                return true;
            }
            final String cleanedTitle = title.getText().toString().trim();
            final String cleanedMessage = message.getText().toString().trim().replace("\n", "<br />");

            if(TextUtils.isEmpty(cleanedTitle)) {
                showToast(R.string.titleBlank);
            } else if (isAnnouncement && TextUtils.isEmpty(cleanedMessage)) {
                showToast(R.string.messageBlank);
            } else {
                if(isEditing && !isAnnouncement) {
                    //Is a discussion and we are editing it
                    DiscussionManager.updateDiscussionTopic(getCanvasContext(), discussionTopicHeader.getId(), cleanedTitle, cleanedMessage, threaded.isChecked(), publish.isChecked(), discussionTopicHeaderCanvasCallback);
                } else {
                    if (discussionTopicHeader == null) {
                        //announcements auto publish
                        if (isAnnouncement) {
                            // FIXME: MIGRATION Consider updating to createCourseDiscussion
                            DiscussionManager.createDiscussion(getCanvasContext(), cleanedTitle, cleanedMessage, threaded.isChecked(), isAnnouncement, true, discussionTopicHeaderCanvasCallback);
                        } else {
                            //we haven't created this topic yet, so do it now
                            // FIXME: MIGRATION Consider updating to createCourseDiscussion
                            DiscussionManager.createDiscussion(getCanvasContext(), cleanedTitle, cleanedMessage, threaded.isChecked(), isAnnouncement, publish.isChecked(), discussionTopicHeaderCanvasCallback);
                        }
                    } else {
                        //we're editing this unpublished discussion/announcement, so just update it
                        DiscussionManager.updateDiscussionTopic(getCanvasContext(), discussionTopicHeader.getId(), cleanedTitle, cleanedMessage, threaded.isChecked(), publish.isChecked(), discussionTopicHeaderCanvasCallback);
                    }
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);

        isAnnouncement = extras.getBoolean(Const.ANNOUNCEMENT);
        discussionTopicHeader = extras.getParcelable(Const.DISCUSSION_HEADER);
        isEditing = extras.getBoolean(Const.IN_EDIT_MODE, false);
    }

    public static Bundle createBundle(CanvasContext canvasContext, boolean isAnnouncement) {
        Bundle extras = createBundle(canvasContext);
        extras.putBoolean(Const.ANNOUNCEMENT, isAnnouncement);
        return extras;
    }

    public static Bundle createBundle(CanvasContext canvasContext, boolean isAnnouncement, DiscussionTopicHeader topic) {
        Bundle extras = createBundle(canvasContext);
        extras.putBoolean(Const.ANNOUNCEMENT, isAnnouncement);
        extras.putParcelable(Const.DISCUSSION_HEADER, topic);
        return extras;
    }

    public static Bundle createBundle(CanvasContext canvasContext, boolean isAnnouncement, DiscussionTopicHeader topic, boolean isEditing) {
        Bundle extras = createBundle(canvasContext);
        extras.putBoolean(Const.ANNOUNCEMENT, isAnnouncement);
        extras.putParcelable(Const.DISCUSSION_HEADER, topic);
        extras.putBoolean(Const.IN_EDIT_MODE, isEditing);
        return extras;
    }

    @Override
    public boolean allowBookmarking() {
        return false;
    }
}
