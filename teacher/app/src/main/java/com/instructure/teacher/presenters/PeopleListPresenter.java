/*
 * Copyright (C) 2017 - present  Instructure, Inc.
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
 */

package com.instructure.teacher.presenters;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.RecipientManager;
import com.instructure.canvasapi2.managers.UserManager;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Enrollment;
import com.instructure.canvasapi2.models.Recipient;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.teacher.viewinterface.PeopleListView;

import java.util.ArrayList;
import java.util.List;

import instructure.androidblueprint.SyncPresenter;
import retrofit2.Response;


public class PeopleListPresenter extends SyncPresenter<User, PeopleListView> {


    private ArrayList<CanvasContext> mCanvasContextList = new ArrayList<>();
    private CanvasContext mCanvasContext;
    private ArrayList<User> mUserList = new ArrayList<>();
    private RecipientRunnable mRun;
    //If we try to automate this class the handler might create some issues. Cross that bridge when we come to it
    private Handler mHandler = new Handler();

    public PeopleListPresenter(@NonNull CanvasContext canvasContext) {
        super(User.class);
        mCanvasContext = canvasContext;
    }

    @Override
    public void loadData(boolean forceNetwork) {
        if(forceNetwork){
            mUserList.clear();
        } else {
            if(getData().size() > 0) return;
        }
        onRefreshStarted();
        UserManager.getAllEnrollmentsPeopleList(mCanvasContext, mUserListCallback, forceNetwork);
    }

    @Override
    public void refresh(boolean forceNetwork) {
        if (mCanvasContextList.isEmpty()) {
            onRefreshStarted();
            mUserListCallback.reset();
            mUserList.clear();
            clearData();
            loadData(forceNetwork);
        } else if(getViewCallback() != null) {
            getViewCallback().checkIfEmpty();
            getViewCallback().onRefreshFinished();
        }
    }

    private void getGroupUsers(CanvasContext group) {
        UserManager.getAllPeopleList(group, mGroupUserCallback, true);
    }

    private StatusCallback<List<User>> mGroupUserCallback = new StatusCallback<List<User>>() {
        @Override
        public void onResponse(@NonNull Response<List<User>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
            // Group user api doesn't return the enrollment, so we need to add the user from the mUserList
            for (User user : response.body()) {
                User currentUser = mUserList.get(mUserList.indexOf(user));
                getData().addOrUpdate(currentUser);
            }
            // all the users in this group should already be in the user list, so we don't need to add them again
        }

        @Override
        public void onFinished(ApiType type) {
            if(getViewCallback() != null) {
                getViewCallback().checkIfEmpty();
                getViewCallback().onRefreshFinished();
            }
        }
    };

    private StatusCallback<List<User>> mUserListCallback = new StatusCallback<List<User>>() {
        @Override
        public void onResponse(@NonNull Response<List<User>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
            getData().addOrUpdate(response.body());
            mUserList.addAll(response.body());
        }

        @Override
        public void onFinished(ApiType type) {
            if(getViewCallback() != null) {
                getViewCallback().checkIfEmpty();
                getViewCallback().onRefreshFinished();
            }
        }
    };

    /**
     *  Calls our API to query for possible recipients, with the mCurrentConstraint as the search parameter.
     *  This process will "kill" any pending runnables. With a delay of 500ms.
     */
    private void fetchAdditionalRecipients(String constraint){
        if(mRun != null){
            mRun.kill();
            mHandler.removeCallbacks(mRun);

        }
        mRun = new RecipientRunnable(constraint);
        mHandler.post(mRun);
    }

    public void searchPeopleList(String searchTerm) {

        mRecipientCallback.reset();
        mUserList.clear();
        clearData();
        fetchAdditionalRecipients(searchTerm);
    }

    private StatusCallback<List<Recipient>> mRecipientCallback = new StatusCallback<List<Recipient>>() {
        @Override
        public void onResponse(@NonNull Response<List<Recipient>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
            clearData();
            getData().beginBatchedUpdates();
            for (Recipient recipient : response.body()) {
                //convert recipient to user
                User user = convertRecipientToUser(recipient);

                getData().add(user);
            }
            getData().endBatchedUpdates();
        }

        @Override
        public void onFinished(ApiType type) {
            if (getViewCallback() != null) {
                getViewCallback().onRefreshFinished();
                getViewCallback().checkIfEmpty();
            }
        }
    };

    @NonNull
    private User convertRecipientToUser(Recipient recipient) {
        User user = new User();
        user.setAvatarUrl(recipient.getAvatarURL());
        user.setId(recipient.getIdAsLong());
        user.setName(recipient.getName());
        user.setSortableName(recipient.getName());
        //get enrollments
        ArrayList<Enrollment> enrollments = new ArrayList<>();
        if(recipient.getCommonCourses() != null) {
            String[] commonCoursesEnrollments = recipient.getCommonCourses().get(Long.toString(mCanvasContext.getId()));
            if (commonCoursesEnrollments != null) {
                for (String enrollment : commonCoursesEnrollments) {
                    Enrollment newEnrollment = new Enrollment();
                    newEnrollment.setType(enrollment);
                    enrollments.add(newEnrollment);
                }
                user.setEnrollments(enrollments);
            }
        }

        return user;
    }


    @Override
    protected int compare(User item1, User item2) {
        return item1.getSortableName().compareToIgnoreCase(item2.getSortableName());
    }


    @Override
    protected boolean areItemsTheSame(User user1, User user2) {
        return user1.getId() == user2.getId();
    }

    public void setCanvasContextList(ArrayList<CanvasContext> canvasContextList) {
        mCanvasContextList.clear();
        mGroupUserCallback.reset();
        clearData();

        for (CanvasContext canvasContext : canvasContextList) {
            if (CanvasContext.Type.isGroup(canvasContext)) {
                // make api call to get group members
                getGroupUsers(canvasContext);
            }
            // add it to the list so we can search for sections and remember which contexts we have selected if the user re-opens the dialog
            mCanvasContextList.add(canvasContext);

        }

        // we've made api calls to get the groups, now filter the rest
        filterCanvasContexts();
    }

    public ArrayList<CanvasContext> getCanvasContextList() {
        return mCanvasContextList;
    }

    /**
     * Convert the list of CanvasContexts to a list of just ids so the dialog can know which CanvasContexts
     * have been selected
     *
     * @return
     */
    public ArrayList<Long> getCanvasContextListIds() {
        ArrayList<Long> contextIds = new ArrayList<>();
        for(CanvasContext canvasContext : mCanvasContextList) {
            contextIds.add(canvasContext.getId());
        }
        return contextIds;
    }

    public void clearCanvasContextList() {
        mCanvasContextList.clear();
        refresh(false);
    }

    public void filterCanvasContexts() {
        clearData();

        // filter the list based on the user's enrollments
        if (!mCanvasContextList.isEmpty()) {
            // get a list of ids to make it easier to check section enrollments
            ArrayList<Long> contextIds = new ArrayList<>();
            for(CanvasContext canvasContext : mCanvasContextList) {
                if(CanvasContext.Type.isSection(canvasContext)) {
                    contextIds.add(canvasContext.getId());
                }
            }

            for (User user : mUserList) {
                for (Enrollment enrollment: user.getEnrollments()) {
                    if (contextIds.contains(enrollment.getCourseSectionId())) {
                        getData().addOrUpdate(user);
                    }
                }
            }
        } else {
            refresh(false);
        }
    }

    private class RecipientRunnable implements Runnable{
        private boolean isKilled = false;
        private String constraint = "";
        RecipientRunnable(String constraint){
            this.constraint = constraint;
        }

        @Override
        public void run() {
            if(!isKilled && null != constraint && !TextUtils.isEmpty(constraint) && mCanvasContext != null){
                onRefreshStarted();
                RecipientManager.searchAllRecipientsNoSyntheticContexts(true, constraint, mCanvasContext.getContextId(), mRecipientCallback);
            } else {
                if (getViewCallback() != null) {
                    getViewCallback().onRefreshFinished();
                    getViewCallback().checkIfEmpty();
                }
            }
        }

        private void kill(){
            isKilled = true;
        }
    }
}
