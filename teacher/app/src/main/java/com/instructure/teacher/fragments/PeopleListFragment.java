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

package com.instructure.teacher.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.instructure.canvasapi2.models.CanvasContext;
import com.instructure.canvasapi2.models.User;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.pandautils.fragments.BaseSyncFragment;
import com.instructure.pandautils.utils.CanvasContextExtensions;
import com.instructure.pandautils.utils.ColorKeeper;
import com.instructure.pandautils.utils.Const;
import com.instructure.pandautils.utils.ViewStyler;
import com.instructure.teacher.R;
import com.instructure.teacher.adapters.PeopleListRecyclerAdapter;
import com.instructure.teacher.adapters.StudentContextFragment;
import com.instructure.teacher.dialog.PeopleListFilterDialog;
import com.instructure.teacher.factory.PeopleListPresenterFactory;
import com.instructure.teacher.holders.UserViewHolder;
import com.instructure.teacher.interfaces.AdapterToFragmentCallback;
import com.instructure.teacher.presenters.PeopleListPresenter;
import com.instructure.interactions.router.Route;
import com.instructure.teacher.router.RouteMatcher;
import com.instructure.teacher.utils.RecyclerViewUtils;
import com.instructure.teacher.utils.ViewUtils;
import com.instructure.teacher.view.EmptyPandaView;
import com.instructure.teacher.viewinterface.PeopleListView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import instructure.androidblueprint.PresenterFactory;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class PeopleListFragment extends BaseSyncFragment<User, PeopleListPresenter, PeopleListView, UserViewHolder, PeopleListRecyclerAdapter> implements PeopleListView, SearchView.OnQueryTextListener {

    private ArrayList<CanvasContext> mCanvasContextsSelected;

    @BindView(R.id.recyclerView) RecyclerView mRecyclerView;
    @BindView(R.id.emptyPandaView) EmptyPandaView mEmptyPandaView;
    @BindView(R.id.swipeRefreshLayout) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.peopleListToolbar) Toolbar mToolbar;
    @BindView(R.id.peopleFilter) TextView mPeopleFilter;
    @BindView(R.id.clearFilterTextView) TextView mClearFilterTextView;
    @BindView(R.id.filterTitleWrapper) RelativeLayout mFilterTitleWrapper;


    public static PeopleListFragment newInstance(CanvasContext canvasContext) {
        Bundle args = new Bundle();
        args.putParcelable(Const.CANVAS_CONTEXT, canvasContext);
        PeopleListFragment fragment = new PeopleListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int layoutResId() {
        return R.layout.fragment_people_list_layout;
    }

    @Override
    public void onCreateView(View view) {
        ButterKnife.bind(this, view);
    }

    @Override
    protected void onReadySetGo(PeopleListPresenter presenter) {
        if(getRecyclerView().getAdapter() == null) {
            mRecyclerView.setAdapter(getAdapter());
        }
        setupViews();
        presenter.loadData(false);
    }

    @Override
    public boolean onHandleBackPressed() {
        //close the search bar if it's open
        if(mToolbar.getMenu().findItem(R.id.search).isActionViewExpanded()) {
            MenuItemCompat.collapseActionView(mToolbar.getMenu().findItem(R.id.search));
            return true;
        }
        return super.onHandleBackPressed();
    }

    private void setupViews() {
        final CanvasContext canvasContext = getArguments().getParcelable(Const.CANVAS_CONTEXT);
        mToolbar.setTitle(R.string.tab_people);
        mToolbar.setSubtitle(canvasContext.getName());
        if(mToolbar.getMenu().size() == 0) mToolbar.inflateMenu(R.menu.menu_people_list);
        final SearchView searchView = (SearchView) mToolbar.getMenu().findItem(R.id.search).getActionView();

        MenuItemCompat.setOnActionExpandListener(mToolbar.getMenu().findItem(R.id.search), new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mFilterTitleWrapper.setVisibility(View.GONE);
                mSwipeRefreshLayout.setEnabled(false);
                if(mToolbar.getMenu().findItem(R.id.peopleFilter) != null) {
                    mToolbar.getMenu().findItem(R.id.peopleFilter).setVisible(false);
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mFilterTitleWrapper.setVisibility(View.VISIBLE);
                mSwipeRefreshLayout.setEnabled(true);
                if(mToolbar.getMenu().findItem(R.id.peopleFilter) != null) {
                    mToolbar.getMenu().findItem(R.id.peopleFilter).setVisible(true);
                }
                getPresenter().refresh(false);
                return true;
            }
        });

        searchView.setOnQueryTextListener(this);

        if(mToolbar.getMenu().findItem(R.id.peopleFilter) != null) {
            mToolbar.getMenu().findItem(R.id.peopleFilter).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    //let the user select the course/group they want to see
                    PeopleListFilterDialog.getInstance(getActivity().getSupportFragmentManager(), getPresenter().getCanvasContextListIds(), canvasContext, true, new Function1<ArrayList<CanvasContext>, Unit>() {
                                @Override
                                public Unit invoke(ArrayList<CanvasContext> canvasContexts) {
                                    mCanvasContextsSelected = new ArrayList<>();
                                    mCanvasContextsSelected.addAll(canvasContexts);

                                    getPresenter().setCanvasContextList(mCanvasContextsSelected);
                                    setupTitle(canvasContexts);

                                    return null;
                                }
                            }
                    ).show(getActivity().getSupportFragmentManager(), PeopleListFilterDialog.class.getSimpleName());
                    return false;
                }
            });
        }

        mClearFilterTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPeopleFilter.setText(R.string.allPeople);
                getPresenter().clearCanvasContextList();
                mClearFilterTextView.setVisibility(View.GONE);
            }
        });

        setupTitle(getPresenter().getCanvasContextList());
        ViewStyler.themeToolbar(getActivity(), mToolbar, ColorKeeper.INSTANCE.getOrGenerateColor(canvasContext), Color.WHITE);
        ViewUtils.setupToolbarBackButton(mToolbar, this);
    }

    /**
     * Used to set up the title at the top of the recyclerview. If there are filters set it'll use the titles of the canvas contexts
     * as the title. Otherwise it will say "All People"
     *
     * @param canvasContexts
     */
    private void setupTitle(ArrayList<CanvasContext> canvasContexts) {
        if(canvasContexts.isEmpty()) {
            mPeopleFilter.setText(R.string.allPeople);
            mClearFilterTextView.setVisibility(View.GONE);
            return;
        }
        // get the title based on Canvas Contexts selected (groups and/or sections)
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < canvasContexts.size(); i++) {
            title.append(canvasContexts.get(i).getName());
            // only add the comma if it's not the last in the list
            if ((i + 1) < canvasContexts.size()) {
                title.append(", ");
            }
        }

        mPeopleFilter.setText(title.toString().trim());
        mClearFilterTextView.setVisibility(View.VISIBLE);
    }


    @Override
    protected PresenterFactory<PeopleListPresenter> getPresenterFactory() {
        return new PeopleListPresenterFactory((CanvasContext)getArguments().getParcelable(Const.CANVAS_CONTEXT));
    }

    @Override
    protected void onPresenterPrepared(PeopleListPresenter presenter) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(mRootView, getContext(), getAdapter(),
                presenter, R.id.swipeRefreshLayout, R.id.recyclerView, R.id.emptyPandaView, getString(R.string.no_items_to_display_short));
        addSwipeToRefresh(mSwipeRefreshLayout);
    }

    @Override
    protected PeopleListRecyclerAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new PeopleListRecyclerAdapter(getContext(), getPresenter(), new AdapterToFragmentCallback<User>() {
                @Override
                public void onRowClicked(User user, int position) {
                    CanvasContext canvasContext = getArguments().getParcelable(Const.CANVAS_CONTEXT);
                    if (CanvasContextExtensions.isDesigner(canvasContext)) {
                        Toast.makeText(getContext(), R.string.errorIsDesigner, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Bundle bundle =  StudentContextFragment.makeBundle(user.getId(), canvasContext.getId(), true);
                    RouteMatcher.route(getContext(), new Route(null, StudentContextFragment.class, canvasContext,bundle));
                }
            });
        }
        return mAdapter;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        getAdapter().clear();
        getPresenter().searchPeopleList(newText);
        return true;
    }

    @NonNull
    @Override
    protected RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    @Override
    public boolean withPagination() {
        return true;
    }

    @Override
    protected int perPageCount() {
        return ApiPrefs.getPerPageCount();
    }

    @Override
    public void onRefreshFinished() {
        mSwipeRefreshLayout.setRefreshing(false);
        mEmptyPandaView.setVisibility(View.GONE);
    }

    @Override
    public void onRefreshStarted() {
        mEmptyPandaView.setLoading();
        mEmptyPandaView.setVisibility(View.VISIBLE);
    }

    @Override
    public void checkIfEmpty() {
        RecyclerViewUtils.checkIfEmpty(mEmptyPandaView, mRecyclerView, mSwipeRefreshLayout, getAdapter(), getPresenter().isEmpty());
    }
}
