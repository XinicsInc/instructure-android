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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.instructure.candroid.R;
import com.instructure.candroid.adapter.AssignmentDateListRecyclerAdapter;
import com.instructure.candroid.adapter.ExpandableRecyclerAdapter;
import com.instructure.candroid.adapter.TermSpinnerAdapter;
import com.instructure.interactions.Navigation;
import com.instructure.candroid.interfaces.AdapterToAssignmentsCallback;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.interfaces.GradingPeriodsCallback;
import com.instructure.candroid.util.FragUtils;
import com.instructure.candroid.util.Param;
import com.instructure.canvasapi2.StatusCallback;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.models.AssignmentGroup;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.GradingPeriod;
import com.instructure.canvasapi2.models.GradingPeriodResponse;
import com.instructure.canvasapi2.utils.ApiType;
import com.instructure.canvasapi2.utils.LinkHeaders;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.ViewStyler;

import java.util.ArrayList;

import retrofit2.Response;

@PageView(url = "{canvasContext}/assignments")
public class AssignmentListFragment extends ParentFragment {

    private View mRootView;
    private AdapterToAssignmentsCallback mAdapterToAssignmentsCallback;
    private ExpandableRecyclerAdapter<AssignmentGroup, Assignment, RecyclerView.ViewHolder> mRecyclerAdapter;
    private Toolbar mToolbar;
    private Spinner mTermSpinner;
    private RelativeLayout mTermSpinnerLayout;
    private TermSpinnerAdapter mTermAdapter;
    private ArrayList<GradingPeriod> mGradingPeriodsList = new ArrayList<>();
    private GradingPeriod mAllTermsGradingPeriod;
    private StatusCallback<GradingPeriodResponse> mGradingPeriodsCallback;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.MASTER; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.assignments);
    }

    @Override
    protected String getSelectedParamName() {
        return Param.ASSIGNMENT_ID;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getParentFragment() != null && !getParentFragment().getRetainInstance()){
            setRetainInstance(this, true);
        }
        mAllTermsGradingPeriod = new GradingPeriod();
        mAllTermsGradingPeriod.setTitle(getString(R.string.allGradingPeriods));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.assignment_list_layout, container, false);
        mToolbar = mRootView.findViewById(R.id.toolbar);
        setUpCallbacks();
        mAdapterToAssignmentsCallback = new AdapterToAssignmentsCallback() {
            @Override
            public void setTermSpinnerState(boolean isEnabled) {
                mTermSpinner.setEnabled(isEnabled);
                if(mTermAdapter != null){
                    if(isEnabled){
                        mTermAdapter.setIsLoading(false);
                    } else {
                        mTermAdapter.setIsLoading(true);
                    }
                    mTermAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onRowClicked(Assignment assignment, int position, boolean isOpenDetail) {
                Navigation navigation = getNavigation();
                if(navigation != null){
                    Bundle bundle = AssignmentFragment.Companion.createBundle((Course) getCanvasContext(), assignment);
                    navigation.addFragment(
                            FragUtils.getFrag(AssignmentFragment.class, bundle));
                }
            }

            @Override
            public void onRefreshFinished() {
                setRefreshing(false);
            }
        };

        // Just load the AssignmentGroup list in the case that its a Group
        mRecyclerAdapter = new AssignmentDateListRecyclerAdapter(getContext(), getCanvasContext(), mGradingPeriodsCallback, mAdapterToAssignmentsCallback);
        configureRecyclerView(mRootView, getContext(), mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView);

        mTermSpinner = mRootView.findViewById(R.id.termSpinner);
        mTermSpinnerLayout = mRootView.findViewById(R.id.termSpinnerLayout);
        AppBarLayout appBarLayout = mRootView.findViewById(R.id.appbar);
        View shadow = mRootView.findViewById(R.id.shadow);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            shadow.setVisibility(View.GONE);
        } else {
            shadow.setVisibility(View.VISIBLE);
        }

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

        return mRootView;
    }

    @Override
    public void applyTheme() {
        setupToolbarMenu(mToolbar);
        mToolbar.setTitle(title());
        PandaViewUtils.setupToolbarBackButton(mToolbar, this);
        ViewStyler.themeToolbar(getActivity(), mToolbar, getCanvasContext());
    }

    private void setUpCallbacks(){
        /*
         *This code is similar to code in the GradeListFragment.
         *If you make changes here, make sure to check the same callback in the GradeListFrag.
         */
        mGradingPeriodsCallback = new StatusCallback<GradingPeriodResponse>() {
            @Override
            public void onResponse(@NonNull Response<GradingPeriodResponse> response, @NonNull LinkHeaders linkHeaders, @NonNull ApiType type) {
                mGradingPeriodsList = new ArrayList<>();
                mGradingPeriodsList.addAll(response.body().getGradingPeriodList());
                //add "select all" option
                mGradingPeriodsList.add(mAllTermsGradingPeriod);
                mTermAdapter = new TermSpinnerAdapter(getContext(), android.R.layout.simple_spinner_dropdown_item, mGradingPeriodsList);
                mTermSpinner.setAdapter(mTermAdapter);
                mTermSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        if (mTermAdapter.getItem(i).getTitle().equals(getString(R.string.allGradingPeriods))) {
                            ((GradingPeriodsCallback) mRecyclerAdapter).loadAssignment();
                        } else {
                            ((GradingPeriodsCallback) mRecyclerAdapter).loadAssignmentsForGradingPeriod(mTermAdapter.getItem(i).getId(), true);
                            mTermSpinner.setEnabled(false);
                            mTermAdapter.setIsLoading(true);
                            mTermAdapter.notifyDataSetChanged();
                        }
                        ((GradingPeriodsCallback)mRecyclerAdapter).setCurrentGradingPeriod(mTermAdapter.getItem(i));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });

                //If we have a "current" grading period select it
                if(((GradingPeriodsCallback)mRecyclerAdapter).getCurrentGradingPeriod() != null) {
                    int position = mTermAdapter.getPositionForId(((GradingPeriodsCallback)mRecyclerAdapter).getCurrentGradingPeriod().getId());
                    if(position != -1){
                        mTermSpinner.setSelection(position);
                    } else {
                        Toast.makeText(getActivity(), com.instructure.loginapi.login.R.string.errorOccurred, Toast.LENGTH_SHORT).show();
                    }
                }

                mTermSpinnerLayout.setVisibility(View.VISIBLE);
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configureRecyclerView(mRootView, getContext(), mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView);
    }

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);
    }

    @Override
    public boolean allowBookmarking() {
        return true;
    }

    public void updatedAssignment(Assignment assignment) {
        if (mRecyclerAdapter != null) {
            mRecyclerAdapter.addOrUpdateItem(mRecyclerAdapter.getGroup(assignment.getAssignmentGroupId()), assignment);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGradingPeriodsCallback.cancel();
    }
}
