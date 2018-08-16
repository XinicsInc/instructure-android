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
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.instructure.candroid.R;
import com.instructure.candroid.adapter.SyllabusRecyclerAdapter;
import com.instructure.interactions.Navigation;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.util.FragUtils;
import com.instructure.candroid.util.Param;
import com.instructure.candroid.view.ViewUtils;
import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.ScheduleItem;
import com.instructure.canvasapi2.models.Tab;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.pandarecycler.BaseRecyclerAdapter;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.ViewStyler;

@PageView(url = "{canvasContext}/assignments/syllabus")
public class ScheduleListFragment extends ParentFragment {

    private boolean addSyllabus;

    private View mRootView;
    private Toolbar mToolbar;
    private AdapterToFragmentCallback<ScheduleItem> mAdapterToFragmentCallback;
    private BaseRecyclerAdapter mRecyclerAdapter;

    //check to see if we're the syllabus tab or the agenda tab
    public boolean isSyllabus() {
        return addSyllabus;
    }

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.MASTER; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.syllabus);
    }

    @Override
    protected String getSelectedParamName() {
        return Param.EVENT_ID;
    }

    public String getTabId() {
        return Tab.SYLLABUS_ID;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = getLayoutInflater().inflate(R.layout.fragment_list_syllabus, container, false);
        mToolbar = mRootView.findViewById(R.id.toolbar);
        mAdapterToFragmentCallback = new AdapterToFragmentCallback<ScheduleItem>() {
            @Override
            public void onRowClicked(ScheduleItem scheduleItem, int position, boolean isOpenDetail) {
                Navigation navigation = getNavigation();
                if(navigation != null){
                    ParentFragment fragment;

                    if (scheduleItem.getAssignment() != null) {
                        fragment = FragUtils.getFrag(AssignmentFragment.class, AssignmentFragment.Companion.createBundle((Course)getCanvasContext(), scheduleItem.getAssignment()));
                    } else if (scheduleItem.getItemType() == ScheduleItem.Type.TYPE_SYLLABUS) {
                        fragment = FragUtils.getFrag(SyllabusFragment.class, SyllabusFragment.Companion.createBundle((Course) getCanvasContext(), scheduleItem));
                    } else {
                        fragment = FragUtils.getFrag(CalendarEventFragment.class, CalendarEventFragment.createBundle(getCanvasContext(), scheduleItem));
                    }

                    navigation.addFragment(fragment);
                }
            }
            @Override
            public void onRefreshFinished() {
                setRefreshing(false);
            }
        };

        mRecyclerAdapter = new SyllabusRecyclerAdapter(getContext(), getCanvasContext(), mAdapterToFragmentCallback);
        configureRecyclerView(mRootView, getContext(), mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView);

        return mRootView;
    }

    @Override
    public void applyTheme() {
        setupToolbarMenu(mToolbar);
        mToolbar.setTitle(title());
        PandaViewUtils.setupToolbarBackButton(mToolbar, this);
        ViewStyler.themeToolbar(getActivity(), mToolbar, getCanvasContext());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configureRecyclerView(mRootView, getContext(), mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mRecyclerAdapter != null) {
            ((SyllabusRecyclerAdapter)mRecyclerAdapter).setupCallbacks();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mRecyclerAdapter != null) {
            ((SyllabusRecyclerAdapter) mRecyclerAdapter).removeCallbacks();
        }
    }

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);
        if(extras == null){return;}
        ViewUtils.showCroutonFromBundle(getActivity(), extras);

        addSyllabus = Tab.SYLLABUS_ID.equals(extras.getString(Const.TAB_ID)) || extras.getBoolean(Const.ADD_SYLLABUS);
        if(extras.containsKey(Const.SELECTED_ITEM)) {
            ScheduleItem scheduleItem = extras.getParcelable(Const.SELECTED_ITEM);
            if(scheduleItem != null) {
                setDefaultSelectedId(scheduleItem.getId());
            }
        }
    }

    public static Bundle createBundle(CanvasContext canvasContext, boolean addSyllabus) {
        Bundle bundle = createBundle(canvasContext);
        bundle.putBoolean(Const.ADD_SYLLABUS, addSyllabus);
        return bundle;
    }

    public static Bundle createBundle(CanvasContext canvasContext, boolean addSyllabus, Tab tab) {
        Bundle bundle = createBundle(canvasContext, tab);
        bundle.putBoolean(Const.ADD_SYLLABUS, addSyllabus);
        return bundle;
    }

    public static Bundle createBundle(CanvasContext canvasContext, ScheduleItem selectedItem){
        Bundle bundle = createBundle(canvasContext);
        bundle.putParcelable(Const.SELECTED_ITEM, selectedItem);
        return bundle;
    }

    @Override
    public boolean allowBookmarking() {
        return false;
    }
}
