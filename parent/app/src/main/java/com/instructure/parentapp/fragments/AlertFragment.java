/*
 * Copyright (C) 2016 - present  Instructure, Inc.
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

package com.instructure.parentapp.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import com.instructure.canvasapi2.models.Alert;
import com.instructure.canvasapi2.models.Student;
import com.instructure.canvasapi2.utils.ApiPrefs;
import com.instructure.pandarecycler.decorations.SpacesItemDecoration;
import com.instructure.pandautils.fragments.BaseSyncFragment;
import com.instructure.pandautils.utils.Const;
import com.instructure.parentapp.R;
import com.instructure.parentapp.activity.StudentViewActivity;
import com.instructure.parentapp.adapter.AlertListRecyclerAdapter;
import com.instructure.parentapp.factorys.AlertPresenterFactory;
import com.instructure.parentapp.holders.AlertViewHolder;
import com.instructure.parentapp.interfaces.AdapterToFragmentBadgeCallback;
import com.instructure.parentapp.presenters.AlertPresenter;
import com.instructure.parentapp.util.AnalyticUtils;
import com.instructure.parentapp.util.ApplicationManager;
import com.instructure.parentapp.util.RecyclerViewUtils;
import com.instructure.parentapp.util.RouterUtils;
import com.instructure.parentapp.view.EmptyPandaView;
import com.instructure.parentapp.viewinterface.AlertView;

import butterknife.BindView;
import butterknife.ButterKnife;
import instructure.androidblueprint.PresenterFactory;

/**
 * Copyright (c) 2016 Instructure. All rights reserved.
 */
public class AlertFragment extends BaseSyncFragment<Alert, AlertPresenter, AlertView, AlertViewHolder, AlertListRecyclerAdapter>
        implements AlertView {

    //region Binding

    @BindView(R.id.swipeRefreshLayout) SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.emptyPandaView) EmptyPandaView mEmptyPandaView;
    @BindView(R.id.recyclerView) RecyclerView mRecyclerView;

    //endregion

    private Student mStudent;

    public static AlertFragment newInstance(Student student){
        Bundle args = new Bundle();
        args.putParcelable(Const.STUDENT, student);
        AlertFragment fragment = new AlertFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int layoutResId() {
        return R.layout.fragment_alert;
    }

    @Override
    public void onCreateView(View view) {
        ButterKnife.bind(this, view);
        addSwipeToRefresh(mSwipeRefreshLayout);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStudent = getArguments().getParcelable(Const.STUDENT);
    }

    public void refreshWithStudent(@NonNull Student student, boolean refresh) {
        if(getPresenter() != null) {
            getArguments().putParcelable(Const.STUDENT, student);
            getPresenter().setStudent(student, refresh);
        }
    }

    public Student getStudent() {
        return getPresenter().getStudent();
    }

    @NonNull
    @Override
    public String airwolfDomain() {
        return ApiPrefs.getAirwolfDomain();
    }

    @NonNull
    @Override
    public String parentId() {
        return ApplicationManager.getParentId(getContext());
    }

    @Override
    protected void onReadySetGo(AlertPresenter presenter) {
        mRecyclerView.setAdapter(getAdapter());
        presenter.loadData(false);
    }

    @Override
    protected PresenterFactory<AlertPresenter> getPresenterFactory() {
        return new AlertPresenterFactory(mStudent);
    }

    @Override
    protected void onPresenterPrepared(AlertPresenter presenter) {
        mRecyclerView = RecyclerViewUtils.buildRecyclerView(mRootView, getContext(), getAdapter(),
                presenter, R.id.swipeRefreshLayout, R.id.recyclerView, R.id.emptyPandaView, getString(R.string.noAlerts));
        mRecyclerView.addItemDecoration(new SpacesItemDecoration(getContext(), R.dimen.med_padding));
        addSwipeToRefresh(mSwipeRefreshLayout);
        addPagination();
    }

    @Override
    protected AlertListRecyclerAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new AlertListRecyclerAdapter(getActivity(), getPresenter(), mAdapterToFragmentCallback, mAdapterItemDismissedCallback);
        }
        return mAdapter;
    }

    @Override
    public boolean withPagination() {
        return true;
    }

    @NonNull
    @Override
    protected RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public SwipeRefreshLayout getSwipeRefreshLayout() {
        return mSwipeRefreshLayout;
    }

    public ProgressBar getProgressBar() {
        return mEmptyPandaView.getProgressBar();
    }

    @Override
    protected int perPageCount() {
        return ApiPrefs.getPerPageCount();
    }

    @Override
    public void onRefreshStarted() {
        mEmptyPandaView.setLoading();
    }

    @Override
    public void onRefreshFinished() {
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void checkIfEmpty() {
        RecyclerViewUtils.checkIfEmpty(mEmptyPandaView, mRecyclerView, mSwipeRefreshLayout, getAdapter(), getPresenter().isEmpty());
    }

    @Override
    public void markPositionAsRead(int position) {
        final int itemCount = getAdapter().getItemCount();

        if (position < itemCount) {
            Alert alert = getAdapter().getItemAtPosition(position);
            if (alert != null) {
                alert.setMarkedRead(true);
                getAdapter().notifyItemChanged(position);
            }
        }

        int unreadCount = 0;
        for (int i = 0; i < itemCount; i++) {
            Alert innerAlert = getAdapter().getItemAtPosition(i);
            if (innerAlert != null && !innerAlert.isMarkedRead()) {
                unreadCount++;
            }
            onUpdateUnreadCount(unreadCount);
        }
    }

    private AdapterToFragmentBadgeCallback<Alert> mAdapterToFragmentCallback = new AdapterToFragmentBadgeCallback<Alert>() {
        @Override
        public void onRowClicked(Alert alert, final int position, boolean isOpenDetail) {
            //open various detail views depending on alert
            if (!TextUtils.isEmpty(alert.getAssetUrl())) {
                //If the alert is a course grade alert, we don't want to route the user
                if(alert.getAlertType() != Alert.ALERT_TYPE.COURSE_GRADE_HIGH && alert.getAlertType() != Alert.ALERT_TYPE.COURSE_GRADE_LOW) {
                    AnalyticUtils.trackFlow(AnalyticUtils.ALERT_FLOW, AnalyticUtils.ALERT_ITEM_SELECTED);
                    //note: student is only utilized for assignment routes
                    Student student = ((StudentViewActivity) getActivity()).getCurrentStudent();
                    //We want to provide the airwolf domain here because alerts will always come from airwolf
                    RouterUtils.routeUrl(getActivity(), alert.getAssetUrl(), student, ApiPrefs.getAirwolfDomain(), true);
                }
            }
            //the student should be set in the adapter

            getPresenter().markAlertAsRead(alert.getStringId(), position);
        }
    };

    private AlertListRecyclerAdapter.ItemDismissedInterface mAdapterItemDismissedCallback = new AlertListRecyclerAdapter.ItemDismissedInterface() {
        @Override
        public void itemDismissed(Alert item, AlertViewHolder holder) {
            AnalyticUtils.trackButtonPressed(AnalyticUtils.DISMISS_ALERT);

            getAdapter().remove(item);

            getPresenter().markAlertAsDismissed(item.getStringId());
            if(!item.isMarkedRead()) {
                updateUnreadCount();
            }
        }
    };


    @Override
    public void updateUnreadCount() {
        int unreadCount = 0;
        for(int i = 0; i < getAdapter().getItemCount(); i++) {
            Alert alert = getAdapter().getItemAtPosition(i);
            if(alert != null && !alert.isMarkedRead()) {
                unreadCount++;
            }
        }
        onUpdateUnreadCount(unreadCount);
    }

    @Override
    public void onUpdateUnreadCount(int unreadCount) {
        if (getActivity() instanceof StudentViewActivity) {
            ((StudentViewActivity) getActivity()).updateAlertUnreadCount(unreadCount);
        }
    }

//    @Nullable
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View rootView = LayoutInflater.from(getActivity()).inflate(getRootLayout(), container, false);
//
//        mAlertListRecyclerAdapter = new AlertListRecyclerAdapter(getActivity(), mStudent, new AdapterToFragmentBadgeCallback<Alert>() {
//            @Override
//            public void onRowClicked(Alert alert, final int position, boolean isOpenDetail) {
//                //open various detail views depending on alert
//                if (!TextUtils.isEmpty(alert.getAssetUrl())) {
//                    //If the alert is a course grade alert, we don't want to route the user
//                    if(alert.getAlertType() != Alert.ALERT_TYPE.COURSE_GRADE_HIGH && alert.getAlertType() != Alert.ALERT_TYPE.COURSE_GRADE_LOW) {
//                        //note: student is only utilized for assignment routes
//                        Student student = ((StudentViewActivity) getActivity()).getCurrentStudent();
//                        //We want to provide the airwolf domain here because alerts will always come from airwolf
//                        RouterUtils.routeUrl(getActivity(), alert.getAssetUrl(), student, APIHelper.getAirwolfDomain(getContext()), true);
//                    }
//                }
//                //the student should be set in the adapter
//
//                AlertManager.markAlertAsRead(
//                        APIHelper.getAirwolfDomain(getContext()),
//                        mStudent.getParentId(),
//                        alert.getStringId(),
//                        new StatusCallback<retrofit2.Response>(){
//                            @Override
//                            public void onResponse(retrofit2.Response<retrofit2.Response> response, LinkHeaders linkHeaders, ApiType type) {
//                                mAlertListRecyclerAdapter.getItemAtPosition(position).setMarkedRead(true);
//                                mAlertListRecyclerAdapter.notifyItemChanged(position);
//                                int unreadCount = 0;
//                                for (int i = 0; i < mAlertListRecyclerAdapter.getItemCount(); i++) {
//                                    if (!mAlertListRecyclerAdapter.getItemAtPosition(i).isMarkedRead()) {
//                                        unreadCount++;
//                                    }
//                                }
//                                onUpdateUnreadCount(unreadCount);
//                            }
//                        }
//                );
//            }
//
//            @Override
//            public void onRefreshFinished() {
//                if (mSwipeRefreshLayout != null) {
//                    mSwipeRefreshLayout.setRefreshing(false);
//                }
//            }
//
//            @Override
//            public void onUpdateUnreadCount(int unreadCount) {
//                if (getActivity() instanceof StudentViewActivity) {
//                    ((StudentViewActivity) getActivity()).updateAlertUnreadCount(unreadCount);
//                }
//            }
//        });
//        RecyclerViewUtils.buildRecyclerView(rootView,
//                getActivity(),
//                mAlertListRecyclerAdapter,
//                R.id.swipeRefreshLayout,
//                R.id.listView,
//                R.id.emptyPandaView,
//                getString(R.string.noAlerts));
//        mSwipeRefreshLayout = (SwipeRefreshLayout)rootView.findViewById(R.id.swipeRefreshLayout);
//        mEmptyPandaView = (EmptyPandaView) rootView.findViewById(R.id.emptyPandaView);
//
//        return rootView;
//    }
//


}
