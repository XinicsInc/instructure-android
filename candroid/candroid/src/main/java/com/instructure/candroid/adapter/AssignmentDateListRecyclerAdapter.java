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

package com.instructure.candroid.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.instructure.candroid.R;
import com.instructure.candroid.binders.AssignmentBinder;
import com.instructure.candroid.binders.EmptyBinder;
import com.instructure.candroid.binders.ExpandableHeaderBinder;
import com.instructure.candroid.holders.AssignmentViewHolder;
import com.instructure.candroid.holders.EmptyViewHolder;
import com.instructure.candroid.holders.ExpandableViewHolder;
import com.instructure.candroid.interfaces.AdapterToAssignmentsCallback;
import com.instructure.candroid.interfaces.GradingPeriodsCallback;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.AssignmentManager;
import com.instructure.canvasapi2.managers.CourseManager;

import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.AssignmentGroup;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.Enrollment;
import com.instructure.canvasapi2.models.GradingPeriod;
import com.instructure.canvasapi2.models.GradingPeriodResponse;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandarecycler.util.GroupSortedList;
import com.instructure.pandarecycler.util.Types;
import com.instructure.pandautils.utils.ColorKeeper;

import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class AssignmentDateListRecyclerAdapter extends ExpandableRecyclerAdapter<AssignmentGroup, Assignment, RecyclerView.ViewHolder> implements GradingPeriodsCallback {

    public static final int HEADER_POSITION_OVERDUE = 0;
    public static final int HEADER_POSITION_UPCOMING = 1;
    public static final int HEADER_POSITION_UNDATED = 2;
    public static final int HEADER_POSITION_PAST = 3;

    private CanvasContext mCanvasContext;
    private AssignmentGroup mOverdue;
    private AssignmentGroup mUpcoming;
    private AssignmentGroup mUndated;
    private AssignmentGroup mPast;
    private AdapterToAssignmentsCallback mAdapterToAssignmentsCallback;
    private StatusCallback<List<AssignmentGroup>> mAssignmentGroupCallback;
    private StatusCallback<GradingPeriodResponse> mGradingPeriodsCallback;

    private GradingPeriod mCurrentGradingPeriod;

    /* For testing purposes only */
    protected AssignmentDateListRecyclerAdapter(Context context) {
        super(context, AssignmentGroup.class, Assignment.class);
    }

    public AssignmentDateListRecyclerAdapter(Context context, CanvasContext canvasContext,
        StatusCallback<GradingPeriodResponse> canvasCallback,
        AdapterToAssignmentsCallback adapterToAssignmentsCallback) {
        super(context, AssignmentGroup.class, Assignment.class);
        mCanvasContext = canvasContext;
        mAdapterToAssignmentsCallback = adapterToAssignmentsCallback;
        mGradingPeriodsCallback = canvasCallback;
        mOverdue = new AssignmentGroup(context.getString(R.string.overdueAssignments), HEADER_POSITION_OVERDUE);
        mUpcoming = new AssignmentGroup(context.getString(R.string.upcomingAssignments), HEADER_POSITION_UPCOMING);
        mUndated = new AssignmentGroup(context.getString(R.string.undatedAssignments), HEADER_POSITION_UNDATED);
        mPast = new AssignmentGroup(context.getString(R.string.pastAssignments), HEADER_POSITION_PAST);
        setExpandedByDefault(true);
        setDisplayEmptyCell(true);
        loadData();
    }

    @Override
    public void setupCallbacks() {
        mAssignmentGroupCallback = new StatusCallback<List<AssignmentGroup>>() {

            @Override
            public void onResponse(@NonNull Response<List<AssignmentGroup>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                addAndSortAssignments(response.body());
                mAdapterToAssignmentsCallback.onRefreshFinished();
                mAdapterToAssignmentsCallback.setTermSpinnerState(true);
            }

            @Override
            public void onFail(@Nullable Call<List<AssignmentGroup>> call, @NonNull Throwable error, @Nullable Response response) {
                mAdapterToAssignmentsCallback.setTermSpinnerState(true);
            }

            @Override
            public void onFinished(ApiType type) {
                AssignmentDateListRecyclerAdapter.this.onCallbackFinished(type);
            }
        };


    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View v, int viewType) {
        if (viewType == Types.TYPE_HEADER) {
            return new ExpandableViewHolder(v);
        } else if (viewType == Types.TYPE_EMPTY_CELL){
            return new EmptyViewHolder(v);
        } else {
            return new AssignmentViewHolder(v);
        }
    }

    @Override
    public int itemLayoutResId(int viewType) {
        if (viewType == Types.TYPE_HEADER) {
            return ExpandableViewHolder.holderResId();
        } else if (viewType == Types.TYPE_EMPTY_CELL){
            return EmptyViewHolder.holderResId();
        } else {
            return AssignmentViewHolder.holderResId();
        }
    }

    @Override
    public void loadData() {
        /*Logic regarding MGP is similar here as it is in both assignment recycler adapters,
            if changes are made here, check if they are needed in the other recycler adapters.*/
        Course course = (Course)mCanvasContext;

        //This check is for the "all grading periods" option
        if (mCurrentGradingPeriod != null && mCurrentGradingPeriod.getTitle() != null
                && mCurrentGradingPeriod.getTitle().equals(getContext().getString(R.string.allGradingPeriods))) {
            loadAssignment();
            return;
        }

        for (Enrollment enrollment : course.getEnrollments()) {
            //Date list is for students
            if(enrollment.isMultipleGradingPeriodsEnabled()) {
                if(mCurrentGradingPeriod == null || mCurrentGradingPeriod.getTitle() == null) {
                    //we load current term by setting up the current GP
                    mCurrentGradingPeriod = new GradingPeriod();
                    mCurrentGradingPeriod.setId(enrollment.getCurrentGradingPeriodId());
                    mCurrentGradingPeriod.setTitle(enrollment.getCurrentGradingPeriodTitle());
                    //request the grading period objects and make the assignment calls
                    //This callback is fulfilled in the grade list fragment.
                    CourseManager.getGradingPeriodsForCourse(mGradingPeriodsCallback, course.getId(), true);
                    //Then we go ahead and load up the assignments for the current period
                    loadAssignmentsForGradingPeriod(mCurrentGradingPeriod.getId(), false);
                    return;
                } else {
                    //Otherwise we load the info from the currently selected grading period
                    loadAssignmentsForGradingPeriod(mCurrentGradingPeriod.getId(), true);
                    return;
                }
            }
        }
        //If we made it this far, MGP is disabled so we just go forward with the standard
        loadAssignment();
    }

    @Override
    public void contextReady() {

    }

    @Override
    public boolean isPaginated() {
        return false;
    }

    @Override
    public void onBindChildHolder(RecyclerView.ViewHolder holder, AssignmentGroup assignmentGroup, Assignment assignment) {
        AssignmentBinder.bind(getContext(), (AssignmentViewHolder) holder, assignment,
                ColorKeeper.getOrGenerateColor(mCanvasContext), mAdapterToAssignmentsCallback);
    }

    @Override
    public void onBindEmptyHolder(RecyclerView.ViewHolder holder, AssignmentGroup assignmentGroup) {
        EmptyBinder.bind((EmptyViewHolder) holder, getContext().getResources().getString(R.string.noAssignmentsInGroup));
    }

    @Override
    public void onBindHeaderHolder(RecyclerView.ViewHolder holder, AssignmentGroup assignmentGroup, boolean isExpanded) {
        ExpandableHeaderBinder.bind(getContext(), mCanvasContext, (ExpandableViewHolder) holder, assignmentGroup, assignmentGroup.getName(), isExpanded, getViewHolderHeaderClicked());
    }

    @Override
    public void loadAssignmentsForGradingPeriod (long gradingPeriodID, boolean refreshFirst) {
        /*Logic regarding MGP is similar here as it is in both assignment recycler adapters,
            if changes are made here, check if they are needed in the other recycler adapters.*/
        if(refreshFirst){
            resetData();
        }

        // Scope assignments if its for a student
        boolean scopeToStudent = ((Course)mCanvasContext).isStudent();
        AssignmentManager.getAssignmentGroupsWithAssignmentsForGradingPeriod(mCanvasContext.getId(), gradingPeriodID, scopeToStudent, isRefresh(), mAssignmentGroupCallback);
    }

    @Override
    public void loadAssignment () {
        AssignmentManager.getAssignmentGroupsWithAssignments(mCanvasContext.getId(), isRefresh(), mAssignmentGroupCallback);
    }

    @Override
    public GradingPeriod getCurrentGradingPeriod() {
        return mCurrentGradingPeriod;
    }

    @Override
    public void setCurrentGradingPeriod(GradingPeriod gradingPeriod) {
        mCurrentGradingPeriod = gradingPeriod;
    }

    private void addAndSortAssignments(List<AssignmentGroup> assignmentGroups) {
        Date today = new Date();
        for (AssignmentGroup assignmentGroup : assignmentGroups) {
            // TODO canHaveOverDueAssignment
            // web does it like this
            // # only handles observer observing one student, this needs to change to handle multiple users in the future
            // canHaveOverdueAssignment = !ENV.current_user_has_been_observer_in_this_course || ENV.observed_student_ids?.length == 1I
            // endtodo
            for (Assignment assignment : assignmentGroup.getAssignments()) {
                Date dueDate = assignment.getDueAt();
                Submission submission = assignment.getSubmission();
                assignment.setSubmission(submission);
                boolean isWithoutGradedSubmission = submission == null || submission.isWithoutGradedSubmission();
                boolean isOverdue = assignment.isAllowedToSubmit() && isWithoutGradedSubmission;
                if (dueDate == null) {
                    addOrUpdateItem(mUndated, assignment);
                } else {
                    if (today.before(dueDate)) {
                        addOrUpdateItem(mUpcoming, assignment);
                    } else if (isOverdue) {
                        addOrUpdateItem(mOverdue, assignment);
                    } else {
                        addOrUpdateItem(mPast, assignment);
                    }
                }
            }
        }
        setAllPagesLoaded(true);
    }

    // region Expandable callbacks

    @Override
    public GroupSortedList.GroupComparatorCallback<AssignmentGroup> createGroupCallback() {
        return new GroupSortedList.GroupComparatorCallback<AssignmentGroup>() {
            @Override
            public int compare(AssignmentGroup o1, AssignmentGroup o2) {
                return o1.getPosition() - o2.getPosition();
            }

            @Override
            public boolean areContentsTheSame(AssignmentGroup oldGroup, AssignmentGroup newGroup) {
                return oldGroup.getName().equals(newGroup.getName());
            }

            @Override
            public boolean areItemsTheSame(AssignmentGroup group1, AssignmentGroup group2) {
                return group1.getId() == group2.getId();
            }

            @Override
            public int getGroupType(AssignmentGroup group) {
                return Types.TYPE_HEADER;
            }

            @Override
            public long getUniqueGroupId(AssignmentGroup group) {
                return group.getId();
            }
        };
    }

    @Override
    public GroupSortedList.ItemComparatorCallback<AssignmentGroup, Assignment> createItemCallback() {
        return new GroupSortedList.ItemComparatorCallback<AssignmentGroup, Assignment>() {
            @Override
            public int compare(AssignmentGroup group, Assignment o1, Assignment o2) {
                int position = group.getPosition();
                if (position == HEADER_POSITION_UNDATED) {
                    return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                } else if (position == HEADER_POSITION_PAST) {
                    return o2.getDueAt().compareTo(o1.getDueAt()); // sort newest date first (o1 and o2 switched places)
                } else {
                    return o1.getDueAt().compareTo(o2.getDueAt()); // sort oldest date first
                }
            }

            @Override
            public boolean areContentsTheSame(Assignment oldItem, Assignment newItem) {
                boolean isSameName = oldItem.getName().equals(newItem.getName());
                if (oldItem.getDueAt() != null && newItem.getDueAt() != null) {
                    return isSameName && oldItem.getDueAt().equals(newItem.getDueAt());
                } else if (oldItem.getDueAt() == null && newItem.getDueAt() != null) {
                    return false;
                } else if (oldItem.getDueAt() != null && newItem.getDueAt() == null) {
                    return false;
                }
                return isSameName;
            }

            @Override
            public boolean areItemsTheSame(Assignment item1, Assignment item2) {
                return item1.getId() == item2.getId();
            }

            @Override
            public int getChildType(AssignmentGroup group, Assignment item) {
                return Types.TYPE_ITEM;
            }

            @Override
            public long getUniqueItemId(Assignment item) {
                return item.getId();
            }
        };
    }

    // endregion
}
