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

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.instructure.candroid.R;
import com.instructure.candroid.adapter.RubricRecyclerAdapter;
import com.instructure.candroid.decorations.RubricDecorator;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.view.EmptyPandaView;
import com.instructure.canvasapi2.models.Assignment;
import com.instructure.canvasapi2.utils.pageview.BeforePageView;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.canvasapi2.utils.pageview.PageViewUrlParam;
import com.instructure.canvasapi2.utils.pageview.PageViewUrlQuery;
import com.instructure.pandarecycler.PandaRecyclerView;
import com.instructure.pandautils.utils.FragmentExtensionsKt;

@PageView(url = "{canvasContext}/assignments/{assignmentId}/submissions/{submissionId}")
public class RubricFragment extends ParentFragment {

    @PageViewUrlParam(name="assignmentId")
    private long getAssignmentId() {
        if (mRecyclerAdapter.getAssignment() != null) {
            return mRecyclerAdapter.getAssignment().getId();
        } else {
            return 0;
        }
    }

    @PageViewUrlParam(name="submissionId")
    private String getSubmissionId() {
        if (mRecyclerAdapter.getAssignment() != null && mRecyclerAdapter.getAssignment().getSubmission() != null) {
            long submissionId = mRecyclerAdapter.getAssignment().getSubmission().getId();
            return submissionId > 0 ? String.valueOf(submissionId) : "";
        }
        return "";
    }

    @PageViewUrlQuery(name = "module_item_id")
    private Long getModuleItemId() {
        return FragmentExtensionsKt.getModuleItemId(this);
    }

    private RubricRecyclerAdapter mRecyclerAdapter;

    public static int getTabTitle() {
        return R.string.assignmentTabGrade;
    }

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.DETAIL; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.grades);
    }

    /**
     * Since this is a nested fragment, the ViewPager calls this method
     *
     * For explanation of isWithinAnotherCallback and isCached refer to comment in {@link com.instructure.candroid.activity.CallbackActivity}
     */
    @BeforePageView
    public void setAssignment(Assignment assignment, boolean isWithinAnotherCallback, boolean isCached) {
        if (mRecyclerAdapter != null) {
            mRecyclerAdapter.setAssignment(assignment);
            mRecyclerAdapter.loadDataChained(isWithinAnotherCallback, isCached);
        }
    }

    /**
     * Since its a nested fragment, handle the no network case manually
     */
    public void onNoNetwork() {
        if (mRecyclerAdapter != null) {
            mRecyclerAdapter.onNoNetwork();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        setRetainInstance(this, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        rootView.findViewById(R.id.fragment_container).setBackgroundColor(Color.WHITE);
        EmptyPandaView emptyRubricView = (EmptyPandaView) rootView.findViewById(R.id.emptyPandaView);

        mRecyclerAdapter = new RubricRecyclerAdapter(getContext(), getCanvasContext(), new AdapterToFragmentCallback() {
            @Override
            public void onRowClicked(Object o, int position, boolean isOpenDetail) {}

            @Override
            public void onRefreshFinished() {
                setRefreshing(false);
            }
        });

        configureRecyclerView(rootView, getContext(), mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView);
        PandaRecyclerView pandaRecyclerView = (PandaRecyclerView) rootView.findViewById(R.id.listView);
        pandaRecyclerView.addItemDecoration(new RubricDecorator(getContext()));

        return rootView;
    }

    @Override
    public void applyTheme() {}

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);
    }

    @Override
    public boolean allowBookmarking() {
        return false;
    }
}
