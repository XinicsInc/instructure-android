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
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.instructure.candroid.R;
import com.instructure.candroid.adapter.GradesListRecyclerAdapter;
import com.instructure.candroid.adapter.TermSpinnerAdapter;
import com.instructure.interactions.Navigation;
import com.instructure.candroid.dialog.WhatIfDialogStyled;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.util.FragUtils;
import com.instructure.candroid.util.Param;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.AssignmentGroup;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.CourseGrade;
import com.instructure.canvasapi2.models.GradingPeriod;
import com.instructure.canvasapi2.models.GradingPeriodResponse;
import com.instructure.canvasapi2.models.Submission;
import com.instructure.canvasapi2.models.Tab;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.canvasapi2.utils.NumberHelper;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.ViewStyler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;

import retrofit2.Response;

@PageView(url = "{canvasContext}/grades")
public class GradesListFragment extends ParentFragment {

    private View rootView;
    private TextView totalGradeView;
    private ImageView lockedGradeImage;
    private AppCompatSpinner termSpinner;
    private TermSpinnerAdapter termAdapter;
    private ArrayList<GradingPeriod> gradingPeriodsList = new ArrayList<>();

    private LinearLayout toggleGradeView;
    private CheckBox showBasedOnGradedAssignmentsCB;
    private LinearLayout toggleWhatIfScores;
    private CheckBox showWhatIfCheckbox;
    private Toolbar toolbar;
    private boolean isWhatIfGrading = false;

    private Course course;
    private GradingPeriod allTermsGradingPeriod;
    private GradesListRecyclerAdapter recyclerAdapter;

    // callbacks
    private WhatIfDialogStyled.WhatIfDialogCallback dialogStyled;
    private AdapterToFragmentCallback<Assignment> adapterToFragmentCallback;
    private GradesListRecyclerAdapter.AdapterToGradesCallback adapterToGradesCallback;
    private StatusCallback<GradingPeriodResponse> gradingPeriodsCallback;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.MASTER; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.grades);
    }

    @Override
    protected String getSelectedParamName() {
        return Param.ASSIGNMENT_ID;
    }

    public String getTabId() {
        return Tab.GRADES_ID;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        allTermsGradingPeriod = new GradingPeriod();
        allTermsGradingPeriod.setTitle(getString(R.string.allGradingPeriods));
        setRetainInstance(this, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_course_grades, container, false);
        toolbar = rootView.findViewById(R.id.toolbar);
        setUpCallbacks();
        configureViews(rootView);

        recyclerAdapter = new GradesListRecyclerAdapter(getContext(), course,
                adapterToFragmentCallback, adapterToGradesCallback, gradingPeriodsCallback, dialogStyled);
        configureRecyclerView(rootView, getContext(), recyclerAdapter, R.id.swipeRefreshLayout, R.id.gradesEmptyPandaView, R.id.listView);

        return rootView;
    }

    @Override
    public void applyTheme() {
        setupToolbarMenu(toolbar);
        toolbar.setTitle(title());
        PandaViewUtils.setupToolbarBackButton(toolbar, this);
        ViewStyler.themeToolbar(getActivity(), toolbar, getCanvasContext());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configureRecyclerView(rootView, getContext(), recyclerAdapter, R.id.swipeRefreshLayout, R.id.gradesEmptyPandaView, R.id.listView);
    }

    public void configureViews(View rootView) {

        //Course will be null here in the case of memory pressure.
        //Not handled automatically as we cast from canvasContext;
        if(course == null){
            return;
        }

        termSpinner = rootView.findViewById(R.id.termSpinner);
        AppBarLayout appBarLayout = rootView.findViewById(R.id.appbar);
        totalGradeView = rootView.findViewById(R.id.txtOverallGrade);
        showBasedOnGradedAssignmentsCB = rootView.findViewById(R.id.showTotalCheckBox);
        showWhatIfCheckbox = rootView.findViewById(R.id.showWhatIfCheckBox);
        toggleGradeView = rootView.findViewById(R.id.grade_toggle_view);
        toggleWhatIfScores = rootView.findViewById(R.id.what_if_view);

        Drawable lockDrawable = ColorKeeper.getColoredDrawable(getContext(),
                R.drawable.vd_lock, getResources().getColor(R.color.canvasTextDark));
        lockedGradeImage = rootView.findViewById(R.id.lockedGradeImage);
        lockedGradeImage.setImageDrawable(lockDrawable);

        setupListeners();
        lockGrade(course.isHideFinalGrades());

        dialogStyled = new WhatIfDialogStyled.WhatIfDialogCallback() {
            @Override
            public void onOkayClick(String whatIf, double total, Assignment assignment, int position) {

                //Create dummy submission for what if grade
                Submission s = new Submission();
                //check to see if grade is empty for reset
                if(TextUtils.isEmpty(whatIf)){
                    assignment.setSubmission(null);
                    recyclerAdapter.getAssignmentsHash().get(assignment.getId()).setSubmission(null);
                }else{
                    s.setScore(Double.parseDouble(whatIf));
                    s.setGrade(whatIf);
                    recyclerAdapter.getAssignmentsHash().get(assignment.getId()).setSubmission(s);
                }

                recyclerAdapter.notifyItemChanged(position);

                //Compute new overall grade
                new ComputeGradesTask(showBasedOnGradedAssignmentsCB.isChecked()).execute();
            }
        };

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
                // workaround for Toolbar not showing with swipe to refresh
                if (i == 0) {
                    setRefreshingEnabled(true);
                } else {
                    setRefreshingEnabled(false);
                }
            }
        });
    }

    private void setupListeners() {
        toggleGradeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBasedOnGradedAssignmentsCB.toggle();
            }
        });

        showBasedOnGradedAssignmentsCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(showWhatIfCheckbox.isChecked()) {
                    new ComputeGradesTask(showBasedOnGradedAssignmentsCB.isChecked()).execute(recyclerAdapter.getAssignmentGroups());
                } else {
                    // isChecked == true -> currentGrades, isChecked == false -> totalGrades
                    totalGradeView.setText(formatGrade(recyclerAdapter.getCourseGrade(), !isChecked));
                }

                lockGrade(course.isHideFinalGrades());
            }
        });

        toggleWhatIfScores.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWhatIfCheckbox.toggle();
            }
        });

        showWhatIfCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                double currentScoreVal;

                if (recyclerAdapter.getCourseGrade() != null && recyclerAdapter.getCourseGrade().getCurrentScore() != null) {
                    currentScoreVal = recyclerAdapter.getCourseGrade().getCurrentScore();
                } else {
                    currentScoreVal = 0.0;
                }

                String currentScore = NumberHelper.doubleToPercentage(currentScoreVal);
                if (!showWhatIfCheckbox.isChecked()) {
                    totalGradeView.setText(currentScore);
                } else if(recyclerAdapter.getWhatIfGrade() != null){
                    totalGradeView.setText(NumberHelper.doubleToPercentage(recyclerAdapter.getWhatIfGrade()));
                }

                //If the user is turning off what if grades we need to do a full refresh, should be
                //cached data, so fast.
                if(!showWhatIfCheckbox.isChecked()) {
                    recyclerAdapter.refresh();
                } else {
                    recyclerAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private void setUpCallbacks(){
        adapterToFragmentCallback = new AdapterToFragmentCallback<Assignment>() {
            @Override
            public void onRowClicked(Assignment assignment, int position, boolean isOpenDetail) {
                Bundle bundle = AssignmentFragment.Companion.createBundle(course, assignment);
                Navigation nav = getNavigation();
                if(nav != null){
                    nav.addFragment(
                            FragUtils.getFrag(AssignmentFragment.class, bundle));
                }
            }

            @Override
            public void onRefreshFinished() {
                setRefreshing(false);
            }
        };
        adapterToGradesCallback = new GradesListRecyclerAdapter.AdapterToGradesCallback() {
            @Override
            public void setTermSpinnerState(boolean isEnabled) {
                if(!isAdded()){
                    return;
                }
                termSpinner.setEnabled(isEnabled);
                if(termAdapter != null){
                    if(isEnabled){
                        termAdapter.setIsLoading(false);
                    } else {
                        termAdapter.setIsLoading(true);
                    }
                    termAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void notifyGradeChanged(CourseGrade courseGrade) {
                if(!isAdded()){
                    return;
                }
                totalGradeView.setText(formatGrade(courseGrade, !showBasedOnGradedAssignmentsCB.isChecked()));
                lockGrade(course.isHideFinalGrades());
            }

            @Override
            public boolean getIsEdit() {
                return showWhatIfCheckbox.isChecked();
            }

            @Override
            public void setIsWhatIfGrading(boolean isWhatIfGrading) {
                if(isWhatIfGrading) {
                    toggleWhatIfScores.setVisibility(View.VISIBLE);
                } else {
                    toggleWhatIfScores.setVisibility(View.GONE);
                }
                GradesListFragment.this.isWhatIfGrading = isWhatIfGrading;
            }
        };


        /*
         *This code is similar to code in the AssignmentListFragment.
         *If you make changes here, make sure to check the same callback in the AssignmentListFrag.
         */
        gradingPeriodsCallback = new StatusCallback<GradingPeriodResponse>() {

            @Override
            public void onResponse(@NonNull Response<GradingPeriodResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                gradingPeriodsList = new ArrayList<>();
                gradingPeriodsList.addAll(response.body().getGradingPeriodList());
                //add "select all" option
                gradingPeriodsList.add(allTermsGradingPeriod);
                termAdapter = new TermSpinnerAdapter(getContext(), android.R.layout.simple_spinner_dropdown_item, gradingPeriodsList);
                termSpinner.setAdapter(termAdapter);
                termSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        //The current item must always be set first
                        recyclerAdapter.setCurrentGradingPeriod(termAdapter.getItem(i));
                        if (termAdapter.getItem(i).getTitle().equals(getString(R.string.allGradingPeriods))) {
                            recyclerAdapter.loadData();
                        } else {
                            recyclerAdapter.loadAssignmentsForGradingPeriod(termAdapter.getItem(i).getId(), true);
                            termSpinner.setEnabled(false);
                            termAdapter.setIsLoading(true);
                            termAdapter.notifyDataSetChanged();
                        }
                        showBasedOnGradedAssignmentsCB.setChecked(true);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });

                //If we have a "current" grading period select it
                if(recyclerAdapter.getCurrentGradingPeriod() != null) {
                    int position = termAdapter.getPositionForId(recyclerAdapter.getCurrentGradingPeriod().getId());
                    if (position != -1) {
                        termSpinner.setSelection(position);
                    } else {
                        Toast.makeText(getActivity(), com.instructure.loginapi.login.R.string.errorOccurred, Toast.LENGTH_SHORT).show();
                    }
                }
                termSpinner.setVisibility(View.VISIBLE);
            }
        };
    }

    private String formatGrade(CourseGrade courseGrade, boolean isFinal) {
        if(courseGrade == null) return getString(R.string.noGradeText);

        if(isFinal) {
            if (courseGrade.getNoFinalGrade()) return getString(R.string.noGradeText);
            return NumberHelper.doubleToPercentage(courseGrade.getFinalScore()) +
                    (courseGrade.hasFinalGradeString() ? String.format(" (%s)", (courseGrade.getFinalGrade())) : "");
        } else {
            if (courseGrade.getNoCurrentGrade()) return getString(R.string.noGradeText);
            return NumberHelper.doubleToPercentage(courseGrade.getCurrentScore()) +
                    (courseGrade.hasCurrentGradeString() ? String.format(" (%s)", (courseGrade.getCurrentGrade())) : "");
        }
    }


    private void lockGrade(boolean isLocked) {
        if (isLocked || (recyclerAdapter != null &&
                        recyclerAdapter.isAllGradingPeriodsSelected() &&
                        !course.isTotalsForAllGradingPeriodsEnabled())) {
            totalGradeView.setVisibility(View.INVISIBLE);
            lockedGradeImage.setVisibility(View.VISIBLE);
            toggleGradeView.setVisibility(View.GONE);
            toggleWhatIfScores.setVisibility(View.GONE);
        } else {
            totalGradeView.setVisibility(View.VISIBLE);
            lockedGradeImage.setVisibility(View.INVISIBLE);
            toggleGradeView.setVisibility(View.VISIBLE);
            if(isWhatIfGrading) toggleWhatIfScores.setVisibility(View.VISIBLE);
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Intent
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);
        course = (Course)getCanvasContext();
    }

    ///////////////////////////////////////////////////////////////////////////
    // ASYNC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * ComputeGradesTask calculates the current total grade based on what if scores and the state
     * of the showTotalGradeCheckbox.
     */
    private class ComputeGradesTask extends AsyncTask<ArrayList<AssignmentGroup>, Void, Double>{
        private boolean isShowTotalGrade;

        public ComputeGradesTask(boolean isShowTotalGrade) {
            this.isShowTotalGrade = isShowTotalGrade;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Double doInBackground(ArrayList<AssignmentGroup>... params) {
            //Calculates grade based on all assignments
            if(!isShowTotalGrade){
                if(course.isApplyAssignmentGroupWeights()){
                    return calcGradesTotal(recyclerAdapter.getAssignmentGroups());
                }else{
                    return calcGradesTotalNoWeight(recyclerAdapter.getAssignmentGroups());
                }
            }else{ //Calculates grade based on only graded assignments
                if(course.isApplyAssignmentGroupWeights()){
                    return calcGradesGraded(recyclerAdapter.getAssignmentGroups());
                }else{
                    return calcGradesGradedNoWeight(recyclerAdapter.getAssignmentGroups());
                }

            }
        }

        /**
         * This helper method is used to calculated a courses total grade
         * based on all assignments, this maps to the online check box in the UNCHECKED state:
         *
         * "Calculated based only on graded assignments"
         *
         * @param groups: A list of assignment groups for the course
         * @return: the grade as a rounded double, IE: 85.6
         */
        private double calcGradesTotal(ArrayList<AssignmentGroup> groups){
            double earnedScore = 0;

            for(AssignmentGroup g : groups){
                double earnedPoints = 0;
                double totalPoints = 0;
                double weight = g.getGroupWeight();
                for (Assignment a : g.getAssignments()){
                    Assignment tempAssignment = recyclerAdapter.getAssignmentsHash().get(a.getId());
                    Submission tempSub = tempAssignment.getSubmission();
                    if(tempSub != null && tempSub.getGrade() != null && !tempAssignment.getSubmissionTypes().contains(null)){
                        earnedPoints += tempSub.getScore();
                    }
                    totalPoints += tempAssignment.getPointsPossible();
                }

                if(totalPoints != 0 && earnedPoints != 0){
                    earnedScore += (earnedPoints / totalPoints) * (weight); //Cumulative
                }
            }

            return (round(earnedScore, 2));
        }

        /**
         * This helper method is used to calculated a courses total grade
         * based on all assignments, this maps to the online check box in the CHECKED state:
         *
         * "Calculated based only on graded assignments"
         *
         * @param groups: A list of assignment groups for the course
         * @return: the grade as a rounded double, IE: 85.6
         */
        private double calcGradesGraded(ArrayList<AssignmentGroup> groups){
            double totalWeight = 0;
            double earnedScore = 0;

            for(AssignmentGroup g : groups){
                double totalPoints = 0;
                double earnedPoints = 0;
                double weight = g.getGroupWeight();
                int assignCount = 0;
                boolean flag = true;
                for (Assignment a : g.getAssignments()){
                    Assignment tempAssignment = recyclerAdapter.getAssignmentsHash().get(a.getId());
                    Submission tempSub = tempAssignment.getSubmission();
                    if(tempSub != null
                            && tempSub.getGrade() != null
                            && !tempAssignment.getSubmissionTypes().contains(null)
                            && !Const.PENDING_REVIEW.equals(tempSub.getWorkflowState())){
                        assignCount++; //determines if a group contains assignments
                        totalPoints += tempAssignment.getPointsPossible();
                        earnedPoints += tempSub.getScore();
                    }
                }

                if(totalPoints != 0){
                    earnedScore += (earnedPoints / totalPoints) * (weight);
                }

                    /*
                    In order to appropriately weight assignments when only some of the weight
                    categories contain graded assignments a totalWeight is created, based on the
                    weight of the missing categories.
                    */
                if(assignCount != 0 && flag){
                    totalWeight += weight;
                    flag = false;
                }
            }

            if (totalWeight < 100 && earnedScore != 0){ //Not sure if earnedScore !=0 needed
                earnedScore = (earnedScore/totalWeight) * 100;//Cumulative
            }

            return (round(earnedScore, 2));
        }

        /**
         * This helper method is used to calculated a courses total grade
         * based on all assignments, this maps to the online check box in the UNCHECKED state:
         *
         * "Calculated based only on graded assignments"
         *
         * AND
         *
         * When a course has the API object member "apply_assignment_group_weights" set to false.
         *
         * @param groups: A list of assignment groups for the course
         * @return: the grade as a rounded double, IE: 85.6
         */
        private double calcGradesTotalNoWeight(ArrayList<AssignmentGroup> groups){
            double earnedScore = 0;
            double earnedPoints = 0;
            double totalPoints = 0;
            for(AssignmentGroup g : groups){
                for (Assignment a : g.getAssignments()){
                    Assignment tempAssignment = recyclerAdapter.getAssignmentsHash().get(a.getId());
                    Submission tempSub = tempAssignment.getSubmission();
                    if(tempSub != null
                            && tempSub.getGrade() != null
                            && !tempAssignment.getSubmissionTypes().contains(null)
                            && !Const.PENDING_REVIEW.equals(tempSub.getWorkflowState())){
                        earnedPoints += tempSub.getScore();
                    }
                    totalPoints += tempAssignment.getPointsPossible();
                }
            }
            if(totalPoints != 0 && earnedPoints != 0){
                earnedScore += (earnedPoints / totalPoints) * 100; //Cumulative
            }

            return (round(earnedScore, 2));
        }

        /**
         * This helper method is used to calculated a courses total grade
         * based on all assignments, this maps to the online check box in the CHECKED state:
         *
         * "Calculated based only on graded assignments"
         *
         * AND
         *
         * When a course has the API object member "apply_assignment_group_weights" set to false.
         *
         * @param groups: A list of assignment groups for the course
         * @return: the grade as a rounded double, IE: 85.6
         */
        private double calcGradesGradedNoWeight(ArrayList<AssignmentGroup> groups){
            double earnedScore = 0;
            double totalPoints = 0;
            double earnedPoints = 0;
            for(AssignmentGroup g : groups){
                for (Assignment a : g.getAssignments()){
                    Assignment tempAssignment = recyclerAdapter.getAssignmentsHash().get(a.getId());
                    Submission tempSub = tempAssignment.getSubmission();
                    if(tempSub != null && tempSub.getGrade() != null && !tempAssignment.getSubmissionTypes().contains(null)){
                        totalPoints += tempAssignment.getPointsPossible();
                        earnedPoints += tempSub.getScore();
                    }
                }
            }
            if(totalPoints != 0){
                earnedScore += (earnedPoints / totalPoints) * 100;
            }

            return (round(earnedScore, 2));
        }

        @Override
        protected void onPostExecute(Double aDouble) {
            super.onPostExecute(aDouble);
            recyclerAdapter.setWhatIfGrade(aDouble);
            totalGradeView.setText(NumberHelper.doubleToPercentage(aDouble));
        }
    }

    @Override
    public boolean allowBookmarking() {
        return true;
    }

    @NonNull
    public HashMap<String, String> getParamForBookmark() {
        return super.getParamForBookmark();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recyclerAdapter != null) recyclerAdapter.cancel();
    }
}
