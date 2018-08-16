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
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.instructure.candroid.R;
import com.instructure.candroid.adapter.ModuleListRecyclerAdapter;
import com.instructure.interactions.Navigation;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.interfaces.ModuleAdapterToFragmentCallback;
import com.instructure.candroid.util.FragUtils;
import com.instructure.candroid.util.ModuleProgressionUtility;
import com.instructure.candroid.util.ModuleUtility;
import com.instructure.candroid.util.Param;
import com.instructure.canvasapi2.models.Course;
import com.instructure.canvasapi2.models.ModuleItem;
import com.instructure.canvasapi2.models.ModuleObject;
import com.instructure.canvasapi2.models.Tab;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.ViewStyler;

import java.util.ArrayList;

@PageView(url="modules")
public class ModuleListFragment extends ParentFragment {

    private Toolbar mToolbar;
    private Course mCourse;
    private ModuleListRecyclerAdapter mRecyclerAdapter;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.MASTER; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.modules);
    }

    @Override
    protected String getSelectedParamName() {
        return Param.MODULE_ID;
    }

    public String getTabId() {
        return Tab.MODULES_ID;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(this, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = getLayoutInflater().inflate(R.layout.fragment_module_list, container, false);
        mToolbar = rootView.findViewById(R.id.toolbar);
        CardView cardView = rootView.findViewById(R.id.cardView);
        if(cardView != null) {
            cardView.setCardBackgroundColor(Color.WHITE);
        }
        mRecyclerAdapter = new ModuleListRecyclerAdapter(mCourse, getDefaultSelectedId(), getContext(), getCanvasContext(), new ModuleAdapterToFragmentCallback() {
            @Override
            public void onRowClicked(ModuleObject moduleObject, ModuleItem moduleItem, int position, boolean isOpenDetail) {
                if (moduleItem.getType() != null && moduleItem.getType().equals(ModuleObject.STATE.unlock_requirements.toString())) {
                    return;
                }

                //don't do anything with headers if the user selects it
                if(moduleItem.getType() != null && (moduleItem.getType().equals(ModuleItem.TYPE.SubHeader.toString()))) {
                    return;
                }

                boolean isLocked = ModuleUtility.isGroupLocked(moduleObject);
                if (isLocked) {
                    return;
                }

                //Remove all the subheaders and stuff.
                ArrayList<ModuleObject> groups = mRecyclerAdapter.getGroups();

                ArrayList<ArrayList<ModuleItem>> moduleItemsArray = new ArrayList<>();
                for(int i = 0; i < groups.size(); i++){
                    ArrayList<ModuleItem> moduleItems = mRecyclerAdapter.getItems(groups.get(i));
                    moduleItemsArray.add(moduleItems);
                }
                ModuleProgressionUtility.ModuleHelper moduleHelper = ModuleProgressionUtility.prepareModulesForCourseProgression(getContext(), moduleItem.getId(), groups, moduleItemsArray);

                Navigation navigation = getNavigation();
                if (navigation != null){
                    Bundle bundle = CourseModuleProgressionFragment.createBundle(groups, moduleHelper.strippedModuleItems, mCourse, moduleHelper.newGroupPosition, moduleHelper.newChildPosition);
                    navigation.addFragment(FragUtils.getFrag(CourseModuleProgressionFragment.class, bundle));
                }
            }

            @Override
            public void onRefreshFinished() {
                setRefreshing(false);
            }
        });
        configureRecyclerView(rootView, getContext(), mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView);
        return rootView;
    }

    @Override
    public void applyTheme() {
        mToolbar.setTitle(title());
        PandaViewUtils.setupToolbarBackButton(mToolbar, this);
        ViewStyler.themeToolbar(getActivity(), mToolbar, getCanvasContext());
    }

    public void notifyOfItemChanged(ModuleObject object, ModuleItem item) {
        if(item == null || object == null || mRecyclerAdapter == null) {
            return;
        }
        mRecyclerAdapter.addOrUpdateItem(object, item);
    }

    public void refreshModuleList() {
        mRecyclerAdapter.updateMasteryPathItems();
    }

    @Override
    public void handleIntentExtras(Bundle extras) {
        super.handleIntentExtras(extras);
        if(extras == null){return;}
        mCourse = (Course) getCanvasContext();
    }

    @Override
    public boolean allowBookmarking() {
        return true;
    }
}
