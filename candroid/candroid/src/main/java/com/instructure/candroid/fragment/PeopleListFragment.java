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
import com.instructure.candroid.adapter.PeopleListRecyclerAdapter;
import com.instructure.interactions.Navigation;
import com.instructure.candroid.interfaces.AdapterToFragmentCallback;
import com.instructure.interactions.FragmentInteractions;
import com.instructure.candroid.util.FragUtils;
import com.instructure.candroid.util.Param;
import com.instructure.canvasapi2.models.Tab;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.utils.pageview.PageView;
import com.instructure.pandautils.utils.PandaViewUtils;
import com.instructure.pandautils.utils.ViewStyler;

@PageView(url="{canvasContext}/users")
public class PeopleListFragment extends ParentFragment {

    private View mRootView;
    private Toolbar mToolbar;
    private PeopleListRecyclerAdapter mRecyclerAdapter;
    private AdapterToFragmentCallback<User> mAdapterToFragmentCallback;

    @Override
    @NonNull
    public FragmentInteractions.Placement getFragmentPlacement() {return FragmentInteractions.Placement.MASTER; }

    @Override
    @NonNull
    public String title() {
        return getString(R.string.coursePeople);
    }

    @Override
    protected String getSelectedParamName() {
        return Param.USER_ID;
    }

    public String getTabId() {
        return Tab.PEOPLE_ID;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mAdapterToFragmentCallback = new AdapterToFragmentCallback<User>() {
            @Override
            public void onRowClicked(User user, int position, boolean isOpenDetail) {
                Navigation navigation = getNavigation();
                if(navigation != null) {
                    navigation.addFragment(
                            FragUtils.getFrag(PeopleDetailsFragment.class, PeopleDetailsFragment.createBundle(user, getCanvasContext())));
                }
            }

            @Override
            public void onRefreshFinished() {
                setRefreshing(false);
            }
        };

        mRootView = getLayoutInflater().inflate(R.layout.fragment_people_list, container, false);
        mToolbar = mRootView.findViewById(R.id.toolbar);
        CardView cardView = mRootView.findViewById(R.id.cardView);
        if(cardView != null) {
            cardView.setCardBackgroundColor(Color.WHITE);
        }
        mRecyclerAdapter = new PeopleListRecyclerAdapter(getContext(), getCanvasContext(), mAdapterToFragmentCallback);
        configureRecyclerView(mRootView, getContext(), mRecyclerAdapter, R.id.swipeRefreshLayout, R.id.emptyPandaView, R.id.listView);

        return mRootView;
    }

    @Override
    public void applyTheme() {
        mToolbar.setTitle(title());
        PandaViewUtils.setupToolbarBackButton(mToolbar, this);
        ViewStyler.themeToolbar(getActivity(), mToolbar, getCanvasContext());
    }

    @Override
    public boolean allowBookmarking() {
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mRecyclerAdapter != null) mRecyclerAdapter.cancel();
    }
}
