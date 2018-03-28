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
import com.instructure.candroid.binders.EmptyBinder;
import com.instructure.candroid.binders.ExpandableHeaderBinder;
import com.instructure.candroid.binders.GradeBinder;
import com.instructure.candroid.dialog.WhatIfDialogStyled;
import com.instructure.candroid.holders.EmptyViewHolder;
import com.instructure.candroid.holders.ExpandableViewHolder;
import com.instructure.candroid.holders.GradeViewHolder;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.managers.AssignmentManager;
import com.instructure.canvasapi2.managers.CourseManager;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.AssignmentGroup;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.CourseGrade;
import com.instructure.canvasapi2.models.Enrollment;
import com.instructure.canvasapi2.models.GradingPeriod;
import com.instructure.canvasapi2.models.GradingPeriodResponse;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.pandarecycler.util.GroupSortedList;
import com.instructure.pandarecycler.util.Types;
import com.instructure.pandautils.utils.ColorKeeper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;


public class GradesListRecyclerAdapter extends ExpandableRecyclerAdapter<AssignmentGroup, Assignment, RecyclerView.ViewHolder> {


    private StatusCallback<List<AssignmentGroup>> assignmentGroupCallback;
    private StatusCallback<Course> courseCallback;
    private StatusCallback<GradingPeriodResponse> gradingPeriodsCallback;
    private StatusCallback<List<Enrollment>> enrollmentCallback;

    private AdapterToFragmentCallback<Assignment> adapterToFragmentCallback;
    private AdapterToGradesCallback adapterToGradesCallback;
    private SetSelectedItemCallback selectedItemCallback;
    private WhatIfDialogStyled.WhatIfDialogCallback dialogStyled;

    private CanvasContext canvasContext;
    private ArrayList<AssignmentGroup> assignmentGroups;
    private HashMap<Long, Assignment> assignmentsHash;
    private Double whatIfGrade;
    private GradingPeriod currentGradingPeriod;

    //state for keeping track of grades for what/if and switching between periods
    private CourseGrade courseGrade;

    public interface AdapterToGradesCallback{
        void notifyGradeChanged(CourseGrade courseGrade);
        boolean getIsEdit();
        void setTermSpinnerState(boolean isEnabled);
        void setIsWhatIfGrading(boolean isWhatIfGrading);
    }

    public interface SetSelectedItemCallback{
        void setSelected(int position);
    }

    /* For Testing purposes only */
    protected GradesListRecyclerAdapter(Context context){
        super(context, AssignmentGroup.class, Assignment.class);
    }

    public GradesListRecyclerAdapter(Context context, CanvasContext canvasContext,
        AdapterToFragmentCallback adapterToFragmentCallback,
        AdapterToGradesCallback adapterToGradesCallback,
        StatusCallback<GradingPeriodResponse> gradingPeriodsCallback, WhatIfDialogStyled.WhatIfDialogCallback dialogStyled) {
        super(context, AssignmentGroup.class, Assignment.class);

        this.canvasContext = canvasContext;
        this.adapterToFragmentCallback = adapterToFragmentCallback;
        this.adapterToGradesCallback = adapterToGradesCallback;
        this.gradingPeriodsCallback = gradingPeriodsCallback;
        this.dialogStyled = dialogStyled;

        assignmentGroups = new ArrayList<>();
        assignmentsHash = new HashMap<>();
        setExpandedByDefault(true);

        loadData();
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(View v, int viewType) {
        if (viewType == Types.TYPE_HEADER) {
            return new ExpandableViewHolder(v);
        } else if (viewType == Types.TYPE_EMPTY_CELL) {
            return new EmptyViewHolder(v);
        } else {
            return new GradeViewHolder(v);
        }
    }

    @Override
    public int itemLayoutResId(int viewType) {
        if (viewType == Types.TYPE_HEADER) {
            return ExpandableViewHolder.holderResId();
        } else if (viewType == Types.TYPE_EMPTY_CELL) {
            return EmptyViewHolder.holderResId();
        } else {
            return GradeViewHolder.holderResId();
        }
    }
    @Override
    public void contextReady() {

    }

    @Override
    public void loadData() {
        CourseManager.getCourseWithGrade(canvasContext.getId(), courseCallback, true);
    }

    public void loadAssignmentsForGradingPeriod (long gradingPeriodID, boolean refreshFirst) {
        /*Logic regarding MGP is similar here as it is in both assignment recycler adapters,
            if changes are made here, check if they are needed in the other recycler adapters.*/
        if(refreshFirst){
            resetData();
        }

        // Scope assignments if its for a student
        boolean scopeToStudent = ((Course) canvasContext).isStudent();
        AssignmentManager.getAssignmentGroupsWithAssignmentsForGradingPeriod(canvasContext.getId(), gradingPeriodID, scopeToStudent, isRefresh(), assignmentGroupCallback);

        //Fetch the enrollments associated with the selected gradingPeriodID, these will contain the
        //correct grade for the period
        CourseManager.getEnrollmentsForGradingPeriod(canvasContext.getId(), gradingPeriodID, enrollmentCallback, true);
    }


    public void loadAssignment () {
        // All grading periods and no grading periods are the same case
        courseGrade = ((Course) canvasContext).getCourseGrade(true);
        adapterToGradesCallback.notifyGradeChanged(courseGrade);

        //Standard load assignments, unfiltered
        AssignmentManager.getAssignmentGroupsWithAssignments(canvasContext.getId(), isRefresh(), assignmentGroupCallback);
    }

    @Override
    public void setupCallbacks(){
        /*Logic regarding MGP is similar here as it is in both assignment recycler adapters,
            if changes are made here, check if they are needed in the other recycler adapters.*/
        courseCallback = new StatusCallback<Course>() {

            @Override
            public void onResponse(@NonNull Response<Course> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                Course course = response.body();
                canvasContext = course;

                //We want to disable what if grading if MGP weights are enabled
                if(course.isWeightedGradingPeriods()) {
                    adapterToGradesCallback.setIsWhatIfGrading(false);
                } else {
                    adapterToGradesCallback.setIsWhatIfGrading(true);
                }

                if (isAllGradingPeriodsSelected()) {
                    loadAssignment();
                    return;
                }

                for (Enrollment enrollment : course.getEnrollments()) {
                    if (enrollment.isStudent() && enrollment.isMultipleGradingPeriodsEnabled()) {
                        if(currentGradingPeriod == null || currentGradingPeriod.getTitle() == null) {
                            //we load current term
                            currentGradingPeriod = new GradingPeriod();
                            currentGradingPeriod.setId(enrollment.getCurrentGradingPeriodId());
                            currentGradingPeriod.setTitle(enrollment.getCurrentGradingPeriodTitle());
                            //request the grading period objects and make the assignment calls
                            //This callback is fulfilled in the grade list fragment.
                            CourseManager.getGradingPeriodsForCourse(gradingPeriodsCallback, course.getId(), true);
                            return;
                        } else {
                            //Otherwise we load the info from the current grading period
                            loadAssignmentsForGradingPeriod(currentGradingPeriod.getId(), true);
                            return;
                        }
                    }
                }

                //if we've made it this far, MGP is not enabled, so we do the standard behavior
                loadAssignment();
            }
        };

        assignmentGroupCallback = new StatusCallback<List<AssignmentGroup>>() {
            @Override
            public void onResponse(@NonNull Response<List<AssignmentGroup>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                //we still need to maintain local copies of the assignments/groups for what if grades
                //so we have the assignments Hash and assignments group list
                for (AssignmentGroup group : response.body()) {
                    addOrUpdateAllItems(group, group.getAssignments());
                    for(Assignment assignment : group.getAssignments()){
                        assignmentsHash.put(assignment.getId(), assignment);
                    }
                    if(!assignmentGroups.contains(group)){
                        assignmentGroups.add(group);
                    }
                }
                setAllPagesLoaded(true);

                adapterToFragmentCallback.onRefreshFinished();
            }

            @Override
            public void onFinished(ApiType type) {
                GradesListRecyclerAdapter.this.onCallbackFinished(type);
            }
        };

        enrollmentCallback = new StatusCallback<List<Enrollment>>() {

            @Override
            public void onResponse(@NonNull Response<List<Enrollment>> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                for (Enrollment enrollment : response.body()) {
                    if (enrollment.isStudent() && enrollment.getUserId() == ApiPrefs.getUser().getId()) {
                        courseGrade = ((Course) canvasContext).getCourseGradeFromEnrollment(enrollment, false);
                        adapterToGradesCallback.notifyGradeChanged(courseGrade);
                        //Inform the spinner things are done
                        adapterToGradesCallback.setTermSpinnerState(true);
                        //we need to update the course that the fragment is using
                        ((Course) canvasContext).addEnrollment(enrollment);
                    }
                }
            }

            @Override
            public void onFail(@Nullable Call<List<Enrollment>> call, @NonNull Throwable error, @Nullable Response response) {
                adapterToGradesCallback.setTermSpinnerState(true);
            }

        };

        selectedItemCallback = new SetSelectedItemCallback() {
            @Override
            public void setSelected(int position) {
                setSelectedPosition(position);
            }
        };
    }

    @Override
    public void onBindChildHolder(RecyclerView.ViewHolder holder, AssignmentGroup assignmentGroup, Assignment assignment) {
        boolean isEdit = adapterToGradesCallback.getIsEdit();
        if(isEdit){
            GradeBinder.bind((GradeViewHolder) holder, getContext(), ColorKeeper.getOrGenerateColor(canvasContext), assignmentsHash.get(assignment.getId()), (Course) canvasContext, adapterToGradesCallback.getIsEdit(), dialogStyled, adapterToFragmentCallback, selectedItemCallback);
        } else {
            GradeBinder.bind((GradeViewHolder) holder, getContext(), ColorKeeper.getOrGenerateColor(canvasContext), assignment, (Course) canvasContext, adapterToGradesCallback.getIsEdit(), dialogStyled, adapterToFragmentCallback, selectedItemCallback);
        }
    }

    @Override
    public void onBindHeaderHolder(RecyclerView.ViewHolder holder, AssignmentGroup assignmentGroup, boolean isExpanded) {
        ExpandableHeaderBinder.bind(getContext(), canvasContext, (ExpandableViewHolder) holder, assignmentGroup, assignmentGroup.getName(), isExpanded, getViewHolderHeaderClicked());
    }

    @Override
    public void onBindEmptyHolder(RecyclerView.ViewHolder holder, AssignmentGroup assignmentGroup) {
        EmptyBinder.bind((EmptyViewHolder) holder, getContext().getResources().getString(R.string.noAssignmentsInGroup));
    }

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
                return o1.getPosition() - o2.getPosition();
            }

            @Override
            public boolean areContentsTheSame(Assignment oldItem, Assignment newItem) {
                return compareAssignments(oldItem, newItem);
            }

            @Override
            public boolean areItemsTheSame(Assignment item1, Assignment item2) {
                return item1.getId() == item2.getId();
            }

            @Override
            public long getUniqueItemId(Assignment item) {
                return item.getId();
            }

            @Override
            public int getChildType(AssignmentGroup group, Assignment item) {
                return Types.TYPE_ITEM;
            }
        };
    }

    @Override
    public void resetData() {
        assignmentsHash.clear();
        assignmentGroups.clear();
        super.resetData();
    }

    public ArrayList<AssignmentGroup> getAssignmentGroups(){
        return assignmentGroups;
    }

    public HashMap<Long, Assignment> getAssignmentsHash(){
        return assignmentsHash;
    }

    private boolean compareAssignments(Assignment oldItem, Assignment newItem) {
        boolean isSameName = oldItem.getName().equals(newItem.getName());
        boolean isSameScore = oldItem.getPointsPossible() == newItem.getPointsPossible();
        boolean isSameSubmission = true;
        boolean isSameGrade = true;
        Submission oldSubmission = oldItem.getSubmission();
        Submission newSubmission = newItem.getSubmission();
        if (oldSubmission != null && newSubmission != null) {
            if (oldSubmission.getGrade() != null && newSubmission.getGrade() != null) {
                isSameGrade = oldSubmission.getGrade().equals(newSubmission.getGrade());
            } else if (isNullableChanged(oldSubmission.getGrade(), newSubmission.getGrade())){
                isSameGrade = false;
            }
        }else if (isNullableChanged(oldSubmission, newSubmission)) {
            isSameSubmission = false;
        }
        return isSameName && isSameGrade && isSameScore && isSameSubmission;
    }

    private boolean isNullableChanged(Object o1, Object o2) {
        return (o1 == null && o2 != null) || (o1 !=null && o2 == null);
    }

    public void setWhatIfGrade(Double grade){
        whatIfGrade = grade;
    }

    public Double getWhatIfGrade(){
        return whatIfGrade;
    }

    public void setCurrentGradingPeriod(GradingPeriod gradingPeriod) {
        currentGradingPeriod = gradingPeriod;
    }

    public GradingPeriod getCurrentGradingPeriod() {
        return currentGradingPeriod;
    }

    public CourseGrade getCourseGrade() { return courseGrade; }

    public boolean isAllGradingPeriodsSelected(){
        return currentGradingPeriod != null
                && currentGradingPeriod.getTitle() != null
                && currentGradingPeriod.getTitle().equals(getContext().getString(R.string.allGradingPeriods));
    }

    @Override
    public void cancel() {
        super.cancel();
        assignmentGroupCallback.cancel();
        courseCallback.cancel();
        gradingPeriodsCallback.cancel();
        enrollmentCallback.cancel();
    }
}
